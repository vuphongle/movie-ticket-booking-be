# 🔔 PayOS Webhook Configuration Guide

## 📋 Vấn đề cần giải quyết

Khi user đóng tab sau khi thanh toán thành công trên PayOS, hệ thống không thể cập nhật trạng thái đơn hàng vì:

- **Return URL chỉ hoạt động khi user được redirect về** (đóng tab = không redirect)
- **Webhook là giải pháp**: PayOS server sẽ gọi trực tiếp đến backend của bạn khi có thay đổi trạng thái thanh toán

## 🏗️ Kiến trúc Webhook

```
User -> PayOS Payment Page -> [User closes tab]
                    ↓
              [Payment Success]
                    ↓
    PayOS Server -> POST /api/payos-webhook (Backend)
                    ↓
              Update Order Status
```

## 🔧 Cấu hình Backend (Đã hoàn thành)

### 1. Webhook Endpoint

- **URL**: `POST /api/payos-webhook`
- **Headers**: `x-payos-signature` (cho verify)
- **Body**: JSON data từ PayOS
- **Security**: Verify HMAC-SHA256 signature với `PAYOS_CHECKSUM_KEY`

### 2. Environment Variables

#### Development (Local)

```bash
# .env
PAYOS_CLIENT_ID=your_client_id
PAYOS_API_KEY=your_api_key
PAYOS_CHECKSUM_KEY=your_checksum_key
PAYOS_WEBHOOK_URL=http://localhost:8080/api/payos-webhook
```

⚠️ **Lưu ý**: Localhost không thể nhận webhook từ PayOS. Sử dụng **ngrok** để test:

```bash
# Install ngrok
brew install ngrok

# Run ngrok
ngrok http 8080

# Sử dụng URL ngrok (ví dụ: https://abc123.ngrok.io)
PAYOS_WEBHOOK_URL=https://abc123.ngrok.io/api/payos-webhook
```

#### Production (VPS)

```bash
# .env hoặc biến môi trường
PAYOS_CLIENT_ID=your_client_id
PAYOS_API_KEY=your_api_key
PAYOS_CHECKSUM_KEY=your_checksum_key
PAYOS_WEBHOOK_URL=https://api.yourdomain.com/api/payos-webhook
```

## 🌐 Cấu hình trên PayOS Dashboard

### Bước 1: Truy cập PayOS Dashboard

1. Đăng nhập vào [PayOS Dashboard](https://my.payos.vn/)
2. Chọn ứng dụng của bạn

### Bước 2: Cấu hình Webhook URL

1. Vào mục **Settings** hoặc **Webhook Configuration**
2. Nhập Webhook URL:
   - **Development**: `https://your-ngrok-url.ngrok.io/api/payos-webhook`
   - **Production**: `https://api.yourdomain.com/api/payos-webhook`
3. **Lưu cấu hình**

### Bước 3: Test Webhook

PayOS thường có tính năng test webhook, sử dụng nó để verify:

- Backend nhận được request
- Signature verification thành công
- Response status 200 OK

## 🚀 Deployment trên VPS

### 1. Cấu hình Nginx (nếu sử dụng)

Đảm bảo `/api/payos-webhook` được proxy đúng:

```nginx
# /etc/nginx/sites-available/your-app
server {
    listen 443 ssl;
    server_name api.yourdomain.com;

    # SSL certificates
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Important for webhook
        proxy_set_header x-payos-signature $http_x_payos_signature;
    }
}
```

### 2. Kiểm tra Firewall

Đảm bảo port 443 (HTTPS) mở:

```bash
# Ubuntu/Debian
sudo ufw allow 443/tcp
sudo ufw status

# CentOS/RHEL
sudo firewall-cmd --add-service=https --permanent
sudo firewall-cmd --reload
```

### 3. SSL Certificate (REQUIRED)

PayOS chỉ gửi webhook đến HTTPS endpoints:

```bash
# Sử dụng Let's Encrypt (certbot)
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d api.yourdomain.com
```

### 4. Deploy với Docker Compose

```bash
# 1. Update .env với webhook URL
echo "PAYOS_WEBHOOK_URL=https://api.yourdomain.com/api/payos-webhook" >> .env

# 2. Rebuild và restart
docker-compose down
docker-compose up -d --build

# 3. Kiểm tra logs
docker-compose logs -f backend
```

## 🧪 Testing Webhook

### 1. Local Testing với ngrok

```bash
# Terminal 1: Start backend
./gradlew bootRun

# Terminal 2: Start ngrok
ngrok http 8080

# Terminal 3: Monitor logs
tail -f logs/application.log | grep -i webhook
```

### 2. Production Testing

```bash
# SSH vào VPS
ssh user@your-vps-ip

# Check backend logs
docker-compose logs -f backend | grep -i webhook

# Hoặc
tail -f /var/log/nginx/access.log | grep payos-webhook
```

### 3. Manual Test với curl

```bash
# Generate test signature (sử dụng checksum key của bạn)
curl -X POST https://api.yourdomain.com/api/payos-webhook \
  -H "Content-Type: application/json" \
  -H "x-payos-signature: your-test-signature" \
  -d '{
    "code": "00",
    "desc": "success",
    "data": {
      "orderCode": 123,
      "amount": 50000,
      "status": "PAID"
    }
  }'
```

## 📊 Monitoring & Debugging

### Log Messages to Watch

```log
✅ SUCCESS:
- "Received PayOS webhook"
- "Webhook signature verification: VALID"
- "Updating order X to CONFIRMED via webhook"
- "Order updated successfully"

❌ ERRORS:
- "Missing webhook signature" → Kiểm tra header forwarding trong Nginx
- "Invalid webhook signature" → Kiểm tra PAYOS_CHECKSUM_KEY
- "Error processing PayOS webhook" → Xem chi tiết exception
```

### Common Issues

#### 1. Webhook không được gọi

- ✅ Kiểm tra webhook URL đã cấu hình đúng trên PayOS dashboard
- ✅ Đảm bảo URL sử dụng HTTPS (PayOS require SSL)
- ✅ Kiểm tra firewall/security groups cho phép traffic từ PayOS IPs

#### 2. Signature verification failed

- ✅ Xác nhận `PAYOS_CHECKSUM_KEY` đúng
- ✅ Kiểm tra request body không bị modify bởi proxy/nginx
- ✅ Xem logs để debug signature computation

#### 3. Order không được update

- ✅ Kiểm tra orderCode trong webhook data
- ✅ Verify order tồn tại trong database
- ✅ Xem application logs cho exception details

## 🔒 Security Best Practices

1. **Always verify signature** - Đã implement trong `PayOSService.verifyWebhookSignature()`
2. **Use HTTPS only** - PayOS chỉ gửi đến HTTPS endpoints
3. **Log webhook data** - Để audit và debug (không log sensitive data)
4. **Idempotency** - Xử lý duplicate webhooks (PayOS có thể gửi nhiều lần)
5. **Rate limiting** - Consider rate limiting webhook endpoint nếu cần

## 📝 Checklist Deploy Production

- [ ] SSL certificate đã cài đặt và valid
- [ ] Nginx configured và tested
- [ ] Environment variables đã set trong .env
- [ ] Webhook URL đã cấu hình trên PayOS dashboard
- [ ] Test payment flow hoàn chỉnh (bao gồm đóng tab)
- [ ] Monitoring/logging đã setup
- [ ] Backup & rollback plan sẵn sàng

## 🆘 Support

Nếu gặp vấn đề:

1. Kiểm tra logs: `docker-compose logs -f backend | grep webhook`
2. Verify SSL: `curl -I https://api.yourdomain.com/api/payos-webhook`
3. Test endpoint: Sử dụng PayOS dashboard test webhook feature
4. Contact PayOS support nếu webhook không được gọi

---

**Lưu ý quan trọng**:

- Webhook là bắt buộc để đảm bảo tính nhất quán của payment status
- Return URL chỉ là fallback cho trường hợp user được redirect thành công
- Luôn verify signature để security
