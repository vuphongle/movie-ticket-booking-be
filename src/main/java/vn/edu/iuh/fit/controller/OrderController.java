package vn.edu.iuh.fit.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.enums.OrderStatus;
import vn.edu.iuh.fit.model.request.CreateOrderRequest;
import vn.edu.iuh.fit.model.response.PaymentResponse;
import vn.edu.iuh.fit.service.MailService;
import vn.edu.iuh.fit.service.OrderService;
import vn.edu.iuh.fit.service.PayOSService;
import vn.edu.iuh.fit.service.VNPayService;

import java.util.HashMap;
import java.util.Map;

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
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        String paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod() : "PAYOS";
        PaymentResponse response = orderService.createOrder(request, paymentMethod);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/vnpay-payment")
    public ResponseEntity<?> GetMapping(HttpServletRequest request) {
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
        String redirectUrl = "%s:%s/thanh-toan-don-hang/%s?status=%s"
                .formatted(frontendHost, frontendPort, orderInfo, statusParam);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header("Location", redirectUrl)
                .build();

    }

    @GetMapping("/orders/payos-payment")
    public ResponseEntity<?> handleReturn(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));

        String orderInfo = params.get("order_info");
        boolean valid = payOSService.verifyReturn(params);

        if (valid && "SUCCESS".equals(params.get("status"))) {
            orderService.updateOrderStatus(Integer.valueOf(orderInfo), OrderStatus.CONFIRMED);
        } else {
            orderService.updateOrderStatus(Integer.valueOf(orderInfo), OrderStatus.CANCELLED);
        }

        String redirectUrl = "http://localhost:3000/thanh-toan-don-hang/%s?status=%s"
                .formatted(orderInfo, valid ? "success" : "failed");

        return ResponseEntity.status(HttpStatus.FOUND).header("Location", redirectUrl).build();
    }
}