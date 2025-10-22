# 🔄 PayOS Webhook Implementation - Changes Summary

## 📌 Vấn đề đã giải quyết

**Trước đây**: Khi user đóng tab sau khi thanh toán thành công trên PayOS, đơn hàng không được cập nhật trạng thái vì hệ thống chỉ dựa vào Return URL (redirect).

**Giải pháp**: Triển khai PayOS Webhook để nhận thông báo trực tiếp từ PayOS server khi thanh toán thành công, bất kể user có đóng tab hay không.

## 🎯 Các thay đổi đã thực hiện

### 1. Backend Changes

#### A. `PayOSService.java` - Thêm Webhook Verification

**File**: `src/main/java/vn/edu/iuh/fit/service/PayOSService.java`

**Thêm mới**:

- ✅ `verifyWebhookSignature()` - Verify HMAC-SHA256 signature từ PayOS
- ✅ `processWebhook()` - Parse và xử lý webhook data
- ✅ `computeHmacSha256()` - Tính toán signature
- ✅ `getPaymentInfo()` - Optional method để verify payment từ PayOS API

**Dependencies mới**:

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import vn.payos.type.PaymentLinkData;
```

#### B. `OrderController.java` - Thêm Webhook Endpoint

**File**: `src/main/java/vn/edu/iuh/fit/controller/OrderController.java`

**Thêm endpoint mới**:

```java
@PostMapping("/payos-webhook")
public ResponseEntity<?> handlePayOSWebhook(
    @RequestHeader(value = "x-payos-signature", required = false) String signature,
    @RequestBody String webhookBody)
```

**Tính năng**:

- ✅ Verify webhook signature
- ✅ Parse webhook data
- ✅ Update order status tự động
- ✅ Logging đầy đủ cho debugging
- ✅ Error handling

### 2. Configuration Changes

#### A. `application.properties`

**File**: `src/main/resources/application.properties`

**Thêm**:

```properties
payos.webhook.url=${PAYOS_WEBHOOK_URL:http://localhost:8080/api/payos-webhook}
```

#### B. `application-docker.properties`

**File**: `src/main/resources/application-docker.properties`

**Thêm**:

```properties
payos.webhook.url=${PAYOS_WEBHOOK_URL:http://backend:8080/api/payos-webhook}
```

#### C. `docker-compose.yml`

**Thêm environment variable**:

```yaml
PAYOS_WEBHOOK_URL: ${PAYOS_WEBHOOK_URL:-http://backend:8080/api/payos-webhook}
```

#### D. `.env.example`

**Thêm**:

```bash
PAYOS_WEBHOOK_URL=http://localhost:8080/api/payos-webhook
```

#### E. `.env.prod.template`

**Thêm**:

```bash
PAYOS_WEBHOOK_URL=https://your-domain.com/api/payos-webhook
```

### 3. Documentation

**Tạo mới**: `docs/PAYOS-WEBHOOK-SETUP.md`

- 📖 Hướng dẫn chi tiết cấu hình webhook
- 🚀 Hướng dẫn deploy trên VPS
- 🧪 Hướng dẫn testing
- 🔧 Troubleshooting guide
- 🔒 Security best practices

## 🔄 Luồng hoạt động mới

### Trước đây (chỉ Return URL):

```
User pays -> PayOS -> User redirected -> Backend updates order
                          ❌ User closes tab = No update!
```

### Bây giờ (với Webhook):

```
User pays -> PayOS -> PayOS Server calls webhook -> Backend updates order
                   \                                    ✅ Always updates!
                    -> User redirected (fallback)
```

## 📋 Checklist triển khai

### Development (Local)

- [ ] Copy `.env.example` thành `.env`
- [ ] Điền đầy đủ PayOS credentials
- [ ] Install ngrok: `brew install ngrok`
- [ ] Chạy ngrok: `ngrok http 8080`
- [ ] Update `PAYOS_WEBHOOK_URL` với ngrok URL
- [ ] Cấu hình webhook URL trên PayOS dashboard
- [ ] Test thanh toán và đóng tab

### Production (VPS)

- [ ] Update `.env` hoặc environment variables với domain thật
- [ ] Đảm bảo SSL certificate đã cài đặt (HTTPS required)
- [ ] Cấu hình Nginx proxy cho `/api/payos-webhook`
- [ ] Kiểm tra firewall cho phép HTTPS (port 443)
- [ ] Set `PAYOS_WEBHOOK_URL=https://api.yourdomain.com/api/payos-webhook`
- [ ] Cấu hình webhook URL trên PayOS dashboard
- [ ] Deploy và restart services
- [ ] Test payment flow hoàn chỉnh
- [ ] Monitor logs để verify webhook được gọi

## 🧪 Testing Guide

### 1. Test Local với ngrok

```bash
# Terminal 1
./gradlew bootRun

# Terminal 2
ngrok http 8080
# Copy HTTPS URL (e.g., https://abc123.ngrok.io)

# Update .env
PAYOS_WEBHOOK_URL=https://abc123.ngrok.io/api/payos-webhook

# Restart backend, thử payment và đóng tab
```

### 2. Verify Webhook được gọi

```bash
# Check logs
tail -f logs/application.log | grep -i webhook

# Should see:
# "Received PayOS webhook"
# "Webhook signature verification: VALID"
# "Updating order X to CONFIRMED via webhook"
```

### 3. Test trên Production

```bash
# SSH vào VPS
ssh user@your-vps

# Check logs
docker-compose logs -f backend | grep webhook

# Hoặc kiểm tra nginx logs
tail -f /var/log/nginx/access.log | grep payos-webhook
```

## 🔒 Security Features

1. **Signature Verification**: Mỗi webhook request được verify bằng HMAC-SHA256
2. **HTTPS Only**: PayOS chỉ gửi webhook đến HTTPS endpoints
3. **Logging**: Full audit trail của webhook requests
4. **Error Handling**: Proper error responses cho invalid requests

## 📊 Monitoring

### Log Patterns

**Success**:

```
INFO  - Received PayOS webhook
INFO  - Webhook signature verification: VALID
INFO  - Processing PayOS webhook - OrderCode: 123, Status: PAID
INFO  - Updating order 123 to CONFIRMED via webhook
```

**Errors**:

```
WARN  - Missing webhook signature
ERROR - Invalid webhook signature
ERROR - Error processing webhook body
```

## 🆘 Troubleshooting

### Webhook không được gọi?

1. Kiểm tra webhook URL đã cấu hình đúng trên PayOS dashboard
2. Verify URL sử dụng HTTPS (PayOS require)
3. Test endpoint: `curl https://api.yourdomain.com/api/payos-webhook`
4. Check firewall/security groups

### Signature verification failed?

1. Verify `PAYOS_CHECKSUM_KEY` đúng trong .env
2. Kiểm tra request body không bị modify bởi proxy
3. Xem logs để debug signature computation

### Order không update?

1. Kiểm tra logs xem webhook có được gọi không
2. Verify orderCode trong webhook data
3. Check database connection
4. Xem exception details trong logs

## 📚 Related Documentation

- [PayOS API Documentation](https://payos.vn/docs/api/)
- [Full Deployment Guide](./FULL-DEPLOYMENT-GUIDE.md)
- [Webhook Setup Guide](./PAYOS-WEBHOOK-SETUP.md)

## 🔄 Rollback Plan

Nếu có vấn đề, có thể rollback về version cũ:

```bash
git revert <commit-hash>
docker-compose down
docker-compose up -d --build
```

Hệ thống vẫn hoạt động bình thường với Return URL nếu webhook bị lỗi.

## ✅ Kết luận

Với webhook implementation:

- ✅ Đơn hàng luôn được cập nhật kể cả khi user đóng tab
- ✅ Tăng độ tin cậy của payment flow
- ✅ Giảm friction cho user experience
- ✅ Production-ready với full logging và error handling
- ✅ Secure với signature verification

**Next Steps**: Deploy lên VPS và test thoroughly!
