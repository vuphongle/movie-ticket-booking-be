# ✅ PayOS Webhook - Quick Checklist

## 🎯 TÓM TẮT TÌNH TRẠNG

**Trạng thái:** ✅ ĐÃ SỬA XONG - SẴN SÀNG TEST  
**Ngày:** 22/10/2025

---

## 🔧 NHỮNG GÌ ĐÃ SỬA (Vừa xong)

### 1. ✅ SecurityConfig.java - CRITICAL
**Vấn đề:** Webhook bị block 401/403  
**Đã sửa:** Thêm `.requestMatchers("/api/payos-webhook").permitAll()`

### 2. ✅ .env file
**Vấn đề:** Thiếu PAYOS_WEBHOOK_URL  
**Đã sửa:** Thêm `PAYOS_WEBHOOK_URL=http://localhost:8080/api/payos-webhook`

---

## 📋 BẠN CẦN LÀM GÌ TIẾP THEO?

### OPTION 1: Test Local (Recommended cho development)

```bash
# 1. Install ngrok
brew install ngrok

# 2. Start backend
cd /Users/vuphong/DATN/Code/movie-ticket-booking-be
./gradlew bootRun

# 3. Terminal mới: Start ngrok
ngrok http 8080
# Copy HTTPS URL (vd: https://abc123.ngrok.io)

# 4. Update .env
nano .env
# Thay dòng PAYOS_WEBHOOK_URL=https://abc123.ngrok.io/api/payos-webhook
# Save (Ctrl+X, Y, Enter)

# 5. Restart backend (Ctrl+C terminal backend, chạy lại ./gradlew bootRun)

# 6. Configure PayOS Dashboard
# - Login: https://my.payos.vn/
# - Settings → Webhook URL → https://abc123.ngrok.io/api/payos-webhook
# - Save

# 7. Test script (optional - kiểm tra webhook hoạt động)
./scripts/test-webhook-local.sh

# 8. Monitor logs
tail -f logs/application.log | grep webhook

# 9. TEST THẬT: 
# - Tạo order mới
# - Thanh toán PayOS
# - ĐÓNG TAB ngay
# - Check logs xem order có update không
```

### OPTION 2: Deploy Production (Nếu đã có domain + SSL)

```bash
# 1. SSH vào VPS
ssh user@your-vps

# 2. Update .env production
cd /path/to/movie-ticket-booking-be
nano .env
# Set: PAYOS_WEBHOOK_URL=https://api.yourdomain.com/api/payos-webhook

# 3. Verify SSL
curl -I https://api.yourdomain.com/api/payos-webhook
# Should return 200 or 405 (OK)

# 4. Deploy
docker-compose down
docker-compose up -d --build

# 5. Monitor
docker-compose logs -f backend | grep webhook

# 6. Configure PayOS Dashboard
# URL: https://api.yourdomain.com/api/payos-webhook

# 7. Test payment
```

---

## 🧪 CÁCH VERIFY WEBHOOK HOẠT ĐỘNG

### Logs phải có:
```log
✅ INFO - Received PayOS webhook
✅ INFO - Webhook signature verification: VALID  
✅ INFO - Processing PayOS webhook - OrderCode: 123, Status: PAID
✅ INFO - Updating order 123 to CONFIRMED via webhook
```

### Nếu thấy:
```log
❌ "Missing webhook signature" → Check ngrok/nginx forward header
❌ "Invalid webhook signature" → Check PAYOS_CHECKSUM_KEY
❌ "401 Unauthorized" → BUG ĐÃ SỬA! Restart backend
```

---

## 📁 FILE ĐÃ THAY ĐỔI

1. ✅ `SecurityConfig.java` - Added webhook permitAll
2. ✅ `.env` - Added PAYOS_WEBHOOK_URL
3. ✅ `scripts/test-webhook-local.sh` - NEW test script
4. ✅ `WEBHOOK-STATUS-AND-NEXT-STEPS.md` - Chi tiết đầy đủ

---

## ⏱️ THỜI GIAN ƯỚC TÍNH

- **Local test với ngrok:** 15-30 phút
- **Production deploy:** 30-60 phút (tùy SSL setup)

---

## 🆘 NẾU GẶP VẤN ĐỀ

1. **Xem chi tiết:** `WEBHOOK-STATUS-AND-NEXT-STEPS.md`
2. **Check logs:** `tail -f logs/application.log | grep -i webhook`
3. **Test endpoint:** `./scripts/test-webhook-local.sh`
4. **Documentation:** `docs/PAYOS-WEBHOOK-QUICKSTART.md`

---

## 🎉 TẤT CẢ ĐÃ SẴN SÀNG!

**Bắt đầu từ đâu?**
→ Chạy local test với ngrok (OPTION 1 ở trên)

**Files cần đọc:**
1. File này (quick overview)
2. `WEBHOOK-STATUS-AND-NEXT-STEPS.md` (chi tiết)
3. `docs/PAYOS-WEBHOOK-QUICKSTART.md` (step-by-step)

Good luck! 🚀
