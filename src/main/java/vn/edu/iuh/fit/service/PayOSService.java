package vn.edu.iuh.fit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.config.payment.payos.PayOSConfig;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.PaymentData;
import vn.payos.type.PaymentLinkData;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOSService {

  private final PayOSConfig payOSConfig;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${payos.checksum_key}")
  private String checksumKey;

  public String createOrder(int amount, String orderCode, String returnUrl, Integer expireSeconds) {
    try {
      PayOS payOS = payOSConfig.payOSClient();

      PaymentData.PaymentDataBuilder builder =
          PaymentData.builder()
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

  /** Xác minh phản hồi từ PayOS redirect (nếu cần). */
  public boolean verifyReturn(Map<String, String> params) {
    try {
      String status = params.get("status");
      return "PAID".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
    } catch (Exception e) {
      log.error("Lỗi xác minh phản hồi PayOS", e);
      return false;
    }
  }

  /**
   * Xác minh webhook signature từ PayOS. PayOS gửi signature trong webhook body, và tính signature
   * dựa trên data WITHOUT signature field
   */
  public boolean verifyWebhookSignature(String signature, String requestBody) {
    try {
      // Parse webhook body and remove signature field before computing HMAC
      JsonNode rootNode = objectMapper.readTree(requestBody);

      // Create a copy without signature field
      if (rootNode instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
        com.fasterxml.jackson.databind.node.ObjectNode objectNode =
            (com.fasterxml.jackson.databind.node.ObjectNode) rootNode;
        objectNode.remove("signature");

        // Convert back to string for HMAC computation
        String dataWithoutSignature = objectMapper.writeValueAsString(objectNode);
        log.info("Data for signature verification: {}", dataWithoutSignature);

        String computedSignature = computeHmacSha256(dataWithoutSignature, checksumKey);
        log.info("Computed signature: {}", computedSignature);
        log.info("Received signature: {}", signature);

        boolean isValid = computedSignature.equalsIgnoreCase(signature);
        log.info("Webhook signature verification: {}", isValid ? "VALID" : "INVALID");
        return isValid;
      }

      return false;
    } catch (Exception e) {
      log.error("Error verifying webhook signature", e);
      return false;
    }
  }

  /**
   * Xử lý webhook data từ PayOS
   *
   * @return orderCode nếu thanh toán thành công, null nếu thất bại
   */
  public Long processWebhook(String webhookBody) {
    try {
      JsonNode rootNode = objectMapper.readTree(webhookBody);

      // PayOS webhook structure: { "data": { "orderCode": ..., "amount": ..., "description": ...,
      // ... }, "code": "00", "desc": "success" }
      String code = rootNode.path("code").asText();
      JsonNode dataNode = rootNode.path("data");

      Long orderCode = dataNode.path("orderCode").asLong();
      String status = dataNode.path("status").asText();
      int amount = dataNode.path("amount").asInt();

      log.info(
          "Processing PayOS webhook - OrderCode: {}, Status: {}, Amount: {}, Code: {}",
          orderCode,
          status,
          amount,
          code);

      // Kiểm tra code và status
      if ("00".equals(code)
          && ("PAID".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status))) {
        log.info("Payment successful for order: {}", orderCode);
        return orderCode;
      } else {
        log.warn(
            "Payment not successful - OrderCode: {}, Status: {}, Code: {}",
            orderCode,
            status,
            code);
        return null;
      }
    } catch (Exception e) {
      log.error("Error processing webhook body", e);
      return null;
    }
  }

  /** Tính toán HMAC SHA256 signature */
  private String computeHmacSha256(String data, String key) throws Exception {
    Mac sha256Hmac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
    sha256Hmac.init(secretKey);
    byte[] hash = sha256Hmac.doFinal(data.getBytes("UTF-8"));

    // Convert to hex string
    StringBuilder hexString = new StringBuilder();
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) hexString.append('0');
      hexString.append(hex);
    }
    return hexString.toString();
  }

  /** Lấy thông tin payment từ PayOS API (optional - để verify) */
  public PaymentLinkData getPaymentInfo(Long orderCode) {
    try {
      PayOS payOS = payOSConfig.payOSClient();
      return payOS.getPaymentLinkInformation(orderCode);
    } catch (Exception e) {
      log.error("Error getting payment info for order: {}", orderCode, e);
      return null;
    }
  }
}
