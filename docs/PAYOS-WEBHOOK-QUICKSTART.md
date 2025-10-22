# 🚀 Quick Start: PayOS Webhook Setup

## TL;DR - Các bước nhanh để setup webhook

### 1️⃣ Local Development (với ngrok)

```bash
# 1. Install ngrok
brew install ngrok  # macOS
# hoặc tải từ https://ngrok.com/download

# 2. Update .env file
cd movie-ticket-booking-be
cp .env.example .env
# Edit .env và điền PayOS credentials

# 3. Start backend
./gradlew bootRun

# 4. Start ngrok (terminal khác)
ngrok http 8080
# Copy HTTPS URL (ví dụ: https://abc123.ngrok.io)

# 5. Update .env với ngrok URL
echo "PAYOS_WEBHOOK_URL=https://abc123.ngrok.io/api/payos-webhook" >> .env

# 6. Restart backend
# Ctrl+C và chạy lại ./gradlew bootRun

# 7. Cấu hình trên PayOS Dashboard
# - Login vào https://my.payos.vn/
# - Settings -> Webhook URL
# - Nhập: https://abc123.ngrok.io/api/payos-webhook
# - Save

# 8. Test payment
# - Tạo đơn hàng mới
# - Thanh toán qua PayOS
# - ĐÓng TAB ngay sau khi thanh toán
# - Check logs: tail -f logs/application.log | grep webhook
# - Verify order status đã update
```

### 2️⃣ Production (VPS/Server)

```bash
# 1. SSH vào VPS
ssh user@your-vps-ip

# 2. Update .env
cd /path/to/movie-ticket-booking-be
nano .env
# Thêm dòng:
PAYOS_WEBHOOK_URL=https://api.yourdomain.com/api/payos-webhook

# 3. Verify SSL certificate
curl -I https://api.yourdomain.com/api/payos-webhook
# Should return 200 or 405 (method not allowed is OK)

# 4. Deploy
docker-compose down
docker-compose up -d --build

# 5. Check logs
docker-compose logs -f backend | grep webhook

# 6. Cấu hình PayOS Dashboard
# - Login: https://my.payos.vn/
# - Settings -> Webhook URL
# - Nhập: https://api.yourdomain.com/api/payos-webhook
# - Test webhook (nếu có tính năng)
# - Save

# 7. Test payment thật
# - Tạo order
# - Pay và đóng tab
# - Check order status trong admin
```

## 🔍 Verify Setup Thành Công

### Local (ngrok)

```bash
# Webhook logs should show:
tail -f logs/application.log | grep webhook

# Expected output:
# INFO  - Received PayOS webhook
# INFO  - Webhook signature verification: VALID
# INFO  - Processing PayOS webhook - OrderCode: 123, Status: PAID
# INFO  - Updating order 123 to CONFIRMED via webhook
```

### Production (VPS)

```bash
# Backend logs
docker-compose logs -f backend | grep webhook

# Nginx logs (if applicable)
tail -f /var/log/nginx/access.log | grep payos-webhook

# Xem payment có update không
docker-compose exec db mysql -u user -p -e "SELECT id, status, updated_at FROM orders ORDER BY id DESC LIMIT 5;"
```

## ⚠️ Common Issues

### 1. Ngrok URL thay đổi mỗi lần restart

**Solution**: Sử dụng ngrok account để có fixed domain

```bash
ngrok config add-authtoken YOUR_TOKEN
ngrok http 8080 --domain=your-domain.ngrok-free.app
```

### 2. SSL certificate issue trên VPS

**Solution**: Install Let's Encrypt

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d api.yourdomain.com
```

### 3. Signature verification failed

**Solution**: Check PAYOS_CHECKSUM_KEY

```bash
# Verify trong .env
grep PAYOS_CHECKSUM_KEY .env

# Should match với key trên PayOS dashboard
```

### 4. Firewall blocking webhook

**Solution**: Open port 443

```bash
sudo ufw allow 443/tcp
sudo ufw status
```

## 📝 Environment Variables Required

```bash
# Required PayOS credentials
PAYOS_CLIENT_ID=your_client_id_here
PAYOS_API_KEY=your_api_key_here
PAYOS_CHECKSUM_KEY=your_checksum_key_here

# Webhook URL (change based on environment)
# Local dev with ngrok:
PAYOS_WEBHOOK_URL=https://abc123.ngrok.io/api/payos-webhook

# Production:
PAYOS_WEBHOOK_URL=https://api.yourdomain.com/api/payos-webhook
```

## 🎯 Test Checklist

- [ ] Backend running và accessible
- [ ] Webhook endpoint returns 200 (or 405 for GET)
- [ ] PayOS dashboard có webhook URL configured
- [ ] SSL certificate valid (production)
- [ ] Create test order
- [ ] Pay qua PayOS
- [ ] Đóng tab ngay sau khi pay
- [ ] Order status update thành CONFIRMED
- [ ] Logs show webhook được gọi

## 🔗 Related Docs

- **Chi tiết**: [PAYOS-WEBHOOK-SETUP.md](./PAYOS-WEBHOOK-SETUP.md)
- **Changes**: [PAYOS-WEBHOOK-CHANGES.md](./PAYOS-WEBHOOK-CHANGES.md)
- **Full Deploy**: [FULL-DEPLOYMENT-GUIDE.md](./FULL-DEPLOYMENT-GUIDE.md)

## 💡 Pro Tips

1. **Luôn test với đóng tab** - Đây là use case chính
2. **Monitor logs khi deploy production** - Catch issues early
3. **Keep ngrok running khi dev** - Or use fixed domain
4. **Backup trước khi deploy** - Safety first
5. **Test signature verification** - Security is important

## ✅ Success Criteria

Khi setup thành công:

- ✅ User đóng tab sau pay → Order vẫn update
- ✅ Logs show webhook received & processed
- ✅ No signature verification errors
- ✅ Payment flow smooth

**Happy coding! 🎉**
