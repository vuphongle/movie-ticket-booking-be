package vn.edu.iuh.fit.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.Order;
import vn.edu.iuh.fit.model.enums.OrderStatus;
import vn.edu.iuh.fit.model.request.CreateOrderRequest;
import vn.edu.iuh.fit.model.response.PaymentResponse;
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
  public ResponseEntity<?> GetMapping(HttpServletRequest request) throws Exception {
    int paymentStatus = vnPayService.orderReturn(request);

    String orderInfo = request.getParameter("vnp_OrderInfo");
    String paymentTime = request.getParameter("vnp_PayDate");
    String transactionId = request.getParameter("vnp_TransactionNo");
    String totalPrice = request.getParameter("vnp_Amount");

    if (paymentStatus == 1) {
      orderService.updateOrderStatus(Integer.valueOf(orderInfo), OrderStatus.CONFIRMED);
    } else {
      orderService.updateOrderStatus(Integer.valueOf(orderInfo), OrderStatus.CANCELLED);
    }

    String statusParam = paymentStatus == 1 ? "success" : "failed";
    String redirectUrl =
        "%s:%s/thanh-toan-don-hang/%s?status=%s"
            .formatted(frontendHost, frontendPort, orderInfo, statusParam);

    return ResponseEntity.status(HttpStatus.FOUND).header("Location", redirectUrl).build();
  }

  @GetMapping("/orders/payos-payment")
  public ResponseEntity<?> handlePayOSReturn(HttpServletRequest request) throws Exception {
    Map<String, String> params = new HashMap<>();
    request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));

    String orderCode = params.get("orderCode");
    boolean valid = payOSService.verifyReturn(params);

    if (valid) {
      orderService.updateOrderStatus(Integer.valueOf(orderCode), OrderStatus.CONFIRMED);
    } else {
      orderService.updateOrderStatus(Integer.valueOf(orderCode), OrderStatus.CANCELLED);
    }

    String statusParam = valid ? "success" : "failed";
    String redirectUrl =
        "%s:%s/thanh-toan-don-hang/%s?status=%s"
            .formatted(frontendHost, frontendPort, orderCode, statusParam);

    return ResponseEntity.status(HttpStatus.FOUND).header("Location", redirectUrl).build();
  }

  /**
   * PayOS Webhook endpoint - Nhận thông báo thanh toán từ PayOS
   * Endpoint này được gọi trực tiếp từ PayOS server khi có thay đổi trạng thái thanh toán
   * URL webhook cần được cấu hình trong PayOS dashboard
   */
  @PostMapping("/payos-webhook")
  public ResponseEntity<?> handlePayOSWebhook(
      @RequestHeader(value = "x-payos-signature", required = false) String signature,
      @RequestBody String webhookBody) {
    
    log.info("Received PayOS webhook");
    log.debug("Webhook body: {}", webhookBody);
    log.debug("Signature: {}", signature);

    try {
      // 1. Verify webhook signature
      if (signature == null || signature.isEmpty()) {
        log.warn("Missing webhook signature");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Missing signature"));
      }

      boolean isValid = payOSService.verifyWebhookSignature(signature, webhookBody);
      if (!isValid) {
        log.error("Invalid webhook signature");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Invalid signature"));
      }

      // 2. Process webhook data
      Long orderCode = payOSService.processWebhook(webhookBody);
      
      if (orderCode != null) {
        // Payment successful - update order status
        log.info("Updating order {} to CONFIRMED via webhook", orderCode);
        orderService.updateOrderStatus(orderCode.intValue(), OrderStatus.CONFIRMED);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Order updated successfully",
            "orderCode", orderCode
        ));
      } else {
        // Payment failed or cancelled
        log.warn("Payment not successful for webhook data: {}", webhookBody);
        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "Payment not successful"
        ));
      }
      
    } catch (Exception e) {
      log.error("Error processing PayOS webhook", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/orders/{id}/pdf")
  public ResponseEntity<Resource> downloadOrderPdf(@PathVariable Integer id) throws IOException {
    Order order = orderService.getOrderById(id);
    if (order.getPdfPath() == null) {
      return ResponseEntity.notFound().build();
    }

    Path filePath = Paths.get(order.getPdfPath());
    if (!Files.exists(filePath)) {
      return ResponseEntity.notFound().build();
    }

    Resource resource = new UrlResource(filePath.toUri());
    if (!resource.exists() || !resource.isReadable()) {
      return ResponseEntity.status(500).build();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            "Content-Disposition", "inline; filename=\"" + filePath.getFileName().toString() + "\"")
        .body(resource);
  }

  @GetMapping("/admin/orders")
  public ResponseEntity<?> getAllOrders() {
    return ResponseEntity.ok(orderService.getAllOrders());
  }

  @GetMapping("/admin/orders/{id}")
  public ResponseEntity<?> getOrderById(@PathVariable Integer id) {
    return ResponseEntity.ok(orderService.getOrderById(id));
  }
}
