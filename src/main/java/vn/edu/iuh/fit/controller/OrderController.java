package vn.edu.iuh.fit.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.Order;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.enums.OrderStatus;
import vn.edu.iuh.fit.model.request.CreateOrderRequest;
import vn.edu.iuh.fit.model.response.PaymentResponse;
import vn.edu.iuh.fit.repository.OrderRepository;
import vn.edu.iuh.fit.service.OrderService;
import vn.edu.iuh.fit.service.PayOSService;
import vn.edu.iuh.fit.service.VNPayService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService orderService;
  private final VNPayService vnPayService;
  private final PayOSService payOSService;
  private final OrderRepository orderRepository;

  @Value("${app.frontend.host}")
  private String frontendHost;

  @Value("${app.frontend.port}")
  private String frontendPort;

  @GetMapping("/orders")
  public ResponseEntity<?> getOrdersByCurrentUser() {
    return ResponseEntity.ok(orderService.getOrdersByCurrentUser());
  }

  @GetMapping("/orders/{id}")
  public ResponseEntity<?> GetOrderByIdByCustomer(@PathVariable Integer id) {
    return ResponseEntity.ok(orderService.GetOrderByIdByCustomer(id));
  }

  @PostMapping("/orders")
  public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request)
      throws JsonProcessingException {
    String paymentMethod =
        request.getPaymentMethod() != null ? request.getPaymentMethod() : "PAYOS";
    PaymentResponse response = orderService.createOrder(request, paymentMethod);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/orders/vnpay-payment")
  public ResponseEntity<?> handleVnPayReturn(HttpServletRequest request) throws Exception {
    int paymentStatus = vnPayService.orderReturn(request);

    String orderInfo = request.getParameter("vnp_OrderInfo");
    Integer orderId = Integer.valueOf(orderInfo);

    // Cập nhật trạng thái đơn
    if (paymentStatus == 1) {
      orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
    } else {
      orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    String statusParam = paymentStatus == 1 ? "success" : "failed";

    // Lấy order để kiểm tra pdfPath và platform
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy order với id " + orderId));

    String pdfUrl =
        order.getPdfPath() != null
            ? URLEncoder.encode(order.getPdfPath(), StandardCharsets.UTF_8)
            : "";

    String redirectUrl;
    String platform = order.getPlatform(); // "app" hoặc "web"

    if ("app".equals(platform)) {
      // Redirect về deep link app
      redirectUrl =
          "gocinema://thanh-toan-don-hang/%s?status=%s&pdfUrl=%s"
              .formatted(orderId, statusParam, pdfUrl);
    } else {
      String frontendBaseUrl =
          (frontendPort != null && !frontendPort.trim().isEmpty())
              ? "%s:%s".formatted(frontendHost, frontendPort)
              : frontendHost;

      redirectUrl =
          "%s/thanh-toan-don-hang/%s?status=%s&pdfUrl=%s"
              .formatted(frontendBaseUrl, orderId, statusParam, pdfUrl);
    }

    return ResponseEntity.status(HttpStatus.FOUND).header("Location", redirectUrl).build();
  }

  @GetMapping("/orders/payos-payment")
  public ResponseEntity<?> handlePayOSReturn(HttpServletRequest request) throws Exception {
    Map<String, String> params = new HashMap<>();
    request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));

    String orderCode = params.get("orderCode");
    Integer orderId = Integer.valueOf(orderCode);

    boolean valid = payOSService.verifyReturn(params);

    // Cập nhật trạng thái đơn
    if (valid) {
      orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
    } else {
      orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    String statusParam = valid ? "success" : "failed";

    // Lấy order để kiểm tra pdfPath và platform
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy order với id " + orderId));

    String pdfUrl =
        order.getPdfPath() != null
            ? URLEncoder.encode(order.getPdfPath(), StandardCharsets.UTF_8)
            : "";

    String redirectUrl;
    String platform = order.getPlatform();

    if ("app".equals(platform)) {
      // Redirect về deep link app
      redirectUrl =
          "gocinema://thanh-toan-don-hang/%s?status=%s&pdfUrl=%s"
              .formatted(orderId, statusParam, pdfUrl);
    } else {
      String frontendBaseUrl =
          (frontendPort != null && !frontendPort.trim().isEmpty())
              ? "%s:%s".formatted(frontendHost, frontendPort)
              : frontendHost;

      redirectUrl =
          "%s/thanh-toan-don-hang/%s?status=%s&pdfUrl=%s"
              .formatted(frontendBaseUrl, orderId, statusParam, pdfUrl);
    }

    return ResponseEntity.status(HttpStatus.FOUND).header("Location", redirectUrl).build();
  }

  /**
   * PayOS Webhook endpoint - Nhận thông báo thanh toán từ PayOS Endpoint này được gọi trực tiếp từ
   * PayOS server khi có thay đổi trạng thái thanh toán URL webhook cần được cấu hình trong PayOS
   * dashboard
   */
  @PostMapping("/payos-webhook")
  public ResponseEntity<?> handlePayOSWebhook(
      @RequestHeader(value = "x-payos-signature", required = false) String headerSignature,
      @RequestBody String webhookBody) {

    log.info("Received PayOS webhook");
    log.info("Webhook body: {}", webhookBody);

    try {
      // Parse webhook body to get signature
      JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(webhookBody);
      String signature = rootNode.path("signature").asText();

      log.info("Signature from body: {}", signature);

      // 1. Verify webhook signature
      if (signature == null || signature.isEmpty()) {
        log.warn("Missing webhook signature in body");
        return ResponseEntity.ok().body(Map.of("error", "Missing signature", "success", false));
      }

      boolean isValid = payOSService.verifyWebhookSignature(signature, webhookBody);
      if (!isValid) {
        log.error("Invalid webhook signature");
        return ResponseEntity.ok().body(Map.of("error", "Invalid signature", "success", false));
      }

      // 2. Process webhook data
      Long orderCode = payOSService.processWebhook(webhookBody);

      if (orderCode != null) {
        // Payment successful - update order status
        log.info("Updating order {} to CONFIRMED via webhook", orderCode);
        orderService.updateOrderStatus(orderCode.intValue(), OrderStatus.CONFIRMED);

        return ResponseEntity.ok(
            Map.of(
                "success", true, "message", "Order updated successfully", "orderCode", orderCode));
      } else {
        // Payment failed or cancelled
        log.warn("Payment not successful for webhook data: {}", webhookBody);
        return ResponseEntity.ok(Map.of("success", false, "message", "Payment not successful"));
      }

    } catch (Exception e) {
      log.error("Error processing PayOS webhook", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/orders/{id}/pdf")
  public ResponseEntity<String> getOrderPdf(@PathVariable Integer id) {
    Order order = orderService.getOrderById(id);
    if (order.getPdfPath() == null) {
      return ResponseEntity.notFound().build();
    }

    String pdfUrl = order.getPdfPath();
    return ResponseEntity.ok(pdfUrl);
  }

  @GetMapping("/admin/orders")
  public ResponseEntity<?> getAllOrders() {
    return ResponseEntity.ok(orderService.getAllOrders());
  }

  @GetMapping("/admin/orders/{id}")
  public ResponseEntity<?> getOrderById(@PathVariable Integer id) {
    return ResponseEntity.ok(orderService.getOrderById(id));
  }

  @PutMapping("/admin/orders/{id}/return")
  public ResponseEntity<?> returnOrder(
      @PathVariable Integer id, @RequestBody(required = false) java.util.Map<String, String> body) {
    try {
      String reason = null;
      if (body != null) {
        reason = body.get("reason");
      }
      orderService.returnOrder(id, reason);
      return ResponseEntity.ok(Map.of("message", "Trả hàng thành công"));
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Error returning order", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
    }
  }
}
