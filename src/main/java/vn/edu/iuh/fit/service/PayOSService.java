package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.config.payment.payos.PayOSConfig;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.PaymentData;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOSService {

    private final PayOSConfig payOSConfig;

    public String createOrder(int amount, String orderCode, String returnUrl, Integer expireSeconds) {
        try {
            PayOS payOS = payOSConfig.payOSClient();

            PaymentData.PaymentDataBuilder builder = PaymentData.builder()
                    .orderCode(Long.valueOf(orderCode))
                    .amount(amount)
                    .description("ThanhToanVe " + orderCode)
                    .returnUrl(returnUrl)
                    .cancelUrl(returnUrl + "?status=cancelled");

            if (expireSeconds != null && expireSeconds > 0) {
                long expiredAt = Instant.now().getEpochSecond() + expireSeconds;
                builder.expiredAt(expiredAt);
            }

            PaymentData paymentData = builder.build();
            CheckoutResponseData response = payOS.createPaymentLink(paymentData);
            log.info("Created PayOS payment link for order {}: {}", orderCode, response.getCheckoutUrl());
            return response.getCheckoutUrl();
        } catch (Exception e) {
            log.error("Lỗi khi tạo liên kết thanh toán PayOS", e);
            throw new RuntimeException("Không thể tạo link thanh toán PayOS", e);
        }
    }


    /**
     * Xác minh phản hồi từ PayOS redirect (nếu cần).
     */
    public boolean verifyReturn(Map<String, String> params) {
        try {
            String status = params.get("status");
            return "PAID".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
        } catch (Exception e) {
            log.error("Lỗi xác minh phản hồi PayOS", e);
            return false;
        }
    }
}
