package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.config.payment.payos.PayOSConfig;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class PayOSService {

    public String createOrder(int total, String orderInfo, String urlReturn) {
        // Chuẩn bị tham số
        Map<String, String> params = new TreeMap<>();
        params.put("merchant_id", PayOSConfig.merchantId);
        params.put("amount", String.valueOf(total));
        params.put("order_info", orderInfo);
        params.put("return_url", urlReturn + PayOSConfig.returnUrl);

        // Tạo chuỗi để hash
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
        sb.deleteCharAt(sb.length() - 1); // xóa & cuối

        // Tạo chữ ký
        String signature = PayOSConfig.hmacSHA256(PayOSConfig.secretKey, sb.toString());
        params.put("signature", signature);

        // Build query string
        StringBuilder query = new StringBuilder();
        params.forEach((k, v) -> {
            query.append(URLEncoder.encode(k, StandardCharsets.UTF_8));
            query.append("=");
            query.append(URLEncoder.encode(v, StandardCharsets.UTF_8));
            query.append("&");
        });
        query.deleteCharAt(query.length() - 1);

        return PayOSConfig.apiUrl + "?" + query;
    }

    public boolean verifyReturn(Map<String, String> params) {
        String receivedSignature = params.get("signature");
        params.remove("signature");

        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
        sb.deleteCharAt(sb.length() - 1);

        String calculatedSignature = PayOSConfig.hmacSHA256(PayOSConfig.secretKey, sb.toString());
        return calculatedSignature.equals(receivedSignature);
    }
}