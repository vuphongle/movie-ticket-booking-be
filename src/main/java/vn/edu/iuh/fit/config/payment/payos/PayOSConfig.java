package vn.edu.iuh.fit.config.payment.payos;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class PayOSConfig {
    public static String merchantId = "YOUR_MERCHANT_ID";
    public static String secretKey = "YOUR_SECRET_KEY";
    public static String apiUrl = "https://sandbox.payos.vn/api/payment";
    public static String returnUrl = "/api/orders/payos-payment";

    public static String hmacSHA256(String key, String data) {
        try {
            Mac hmacSHA256mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSHA256mac.init(secretKeySpec);
            byte[] hash = hmacSHA256mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "";
        }
    }
}
