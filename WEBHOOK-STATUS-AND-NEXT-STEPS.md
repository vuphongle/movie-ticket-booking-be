# 🎯 PayOS Webhook - Tình trạng & Các bước tiếp theo

**Ngày kiểm tra:** 22/10/2025  
**Trạng thái:** ✅ Đã sửa các lỗi quan trọng - SẴN SÀNG TEST

---

## ✅ ĐÃ HOÀN THÀNH

### 1. **Backend Implementation** - 100% ✅

- ✅ `PayOSService.java` - Webhook processing methods
- ✅ `OrderController.java` - POST `/api/payos-webhook` endpoint
- ✅ HMAC-SHA256 signature verification
- ✅ PayOS SDK dependency (payos-java:1.0.3)
- ✅ Full logging và error handling
- ✅ Order status update logic

### 2. **Configuration Files** - 100% ✅

- ✅ `application.properties` - Webhook URL config
- ✅ `application-docker.properties` - Docker config
- ✅ `docker-compose.yml` - Environment variables
- ✅ `.env` file - Added PAYOS_WEBHOOK_URL
- ✅ `.env.example` - Template có sẵn

### 3. **Security Configuration** - ✅ VỪA SỬA

- ✅ **FIXED:** Added `/api/payos-webhook` to permitAll() trong SecurityConfig
- ✅ PayOS server có thể gọi webhook không cần authentication

### 4. **Documentation** - 100% ✅

- ✅ PAYOS-WEBHOOK-IMPLEMENTATION.md
- ✅ PAYOS-WEBHOOK-QUICKSTART.md
- ✅ PAYOS-WEBHOOK-SETUP.md
- ✅ PAYOS-WEBHOOK-CHANGES.md

---

## 🔧 VỪA SỬA (22/10/2025)

### 1. **SecurityConfig.java** - CRITICAL FIX ⚠️

**Vấn đề:** Webhook endpoint bị block bởi Spring Security  
**Giải pháp:** Đã thêm permitAll() cho `/api/payos-webhook`

```java
// BEFORE (❌ Blocked):
.requestMatchers("/api/orders/payos-payment").permitAll()
.requestMatchers("/api/v1/chat/**").permitAll()

// AFTER (✅ Fixed):
.requestMatchers("/api/orders/payos-payment").permitAll()
.requestMatchers("/api/payos-webhook").permitAll()  // ← ADDED
.requestMatchers("/api/v1/chat/**").permitAll()
```

### 2. **.env file** - Environment Variable thiếu

**Vấn đề:** Không có PAYOS_WEBHOOK_URL trong .env  
**Giải pháp:** Đã thêm:

```bash
PAYOS_WEBHOOK_URL=http://localhost:8080/api/payos-webhook
```

---

## 📋 CÁC BƯỚC TIẾP THEO

### **BƯỚC 1: Test Local với ngrok** 🧪

#### 1.1. Install ngrok (nếu chưa có)

```bash
# macOS
brew install ngrok

# Hoặc tải từ: https://ngrok.com/download
```

#### 1.2. Start Backend

```bash
cd /Users/vuphong/DATN/Code/movie-ticket-booking-be
./gradlew bootRun
```

#### 1.3. Start ngrok (Terminal khác)

```bash
ngrok http 8080
```

Bạn sẽ thấy output kiểu như:

```
Forwarding  https://abc123.ngrok.io -> http://localhost:8080
```

#### 1.4. Update .env với ngrok URL

```bash
# Stop backend (Ctrl+C)
# Edit .env:
nano .env

# Thay dòng:
PAYOS_WEBHOOK_URL=https://abc123.ngrok.io/api/payos-webhook

# Save và restart backend:
./gradlew bootRun
```

#### 1.5. Cấu hình PayOS Dashboard

1. Truy cập: https://my.payos.vn/
2. Đăng nhập với tài khoản của bạn
3. Vào **Settings** → **Webhook Configuration**
4. Nhập URL: `https://abc123.ngrok.io/api/payos-webhook`
5. **Save** cấu hình

#### 1.6. Test Webhook

```bash
# Terminal 3: Monitor logs
tail -f logs/application.log | grep -i webhook
```

**Test scenario:**

1. Tạo đơn hàng mới trên frontend
2. Chọn thanh toán PayOS
3. Thanh toán thành công
4. **ĐÓNG TAB NGAY** (không đợi redirect)
5. Check logs xem có message:
   - `Received PayOS webhook`
   - `Webhook signature verification: VALID`
   - `Updating order X to CONFIRMED via webhook`
6. Verify order status trong database/admin panel

### **BƯỚC 2: Deploy Production** 🚀

#### 2.1. Prerequisites

- ✅ VPS/Server có domain (ví dụ: api.gocinema.io.vn)
- ✅ SSL certificate đã cài đặt (HTTPS required)
- ✅ Nginx/Reverse proxy configured

#### 2.2. Update Production .env

```bash
# SSH vào VPS
ssh user@your-vps-ip

# Edit .env
cd /path/to/movie-ticket-booking-be
nano .env

# Thay đổi:
PAYOS_WEBHOOK_URL=https://api.gocinema.io.vn/api/payos-webhook
```

#### 2.3. Verify SSL

```bash
curl -I https://api.gocinema.io.vn/api/payos-webhook

# Expected: 200 OK hoặc 405 Method Not Allowed (OK vì PayOS sẽ dùng POST)
```

#### 2.4. Deploy

```bash
# Nếu dùng Docker
docker-compose down
docker-compose up -d --build

# Check logs
docker-compose logs -f backend | grep webhook

# Nếu dùng systemd/manual
./gradlew build
systemctl restart movie-booking-backend
journalctl -u movie-booking-backend -f | grep webhook
```

#### 2.5. Configure PayOS Dashboard (Production)

1. Login: https://my.payos.vn/
2. Settings → Webhook Configuration
3. URL: `https://api.gocinema.io.vn/api/payos-webhook`
4. Test webhook (nếu có feature)
5. Save

#### 2.6. Production Testing

1. Tạo order với số tiền nhỏ (test transaction)
2. Thanh toán và đóng tab
3. Monitor logs
4. Verify order status
5. Kiểm tra email confirmation (nếu có)

---

## 🔍 VERIFICATION CHECKLIST

### Local Development ✓

- [ ] ngrok running và có HTTPS URL
- [ ] Backend running (`./gradlew bootRun`)
- [ ] `.env` có `PAYOS_WEBHOOK_URL` với ngrok URL
- [ ] PayOS dashboard configured với ngrok URL
- [ ] Test payment → đóng tab → order updated
- [ ] Logs hiển thị webhook received & processed

### Production ✓

- [ ] SSL certificate valid và không expired
- [ ] Domain resolve đúng IP
- [ ] Port 443 (HTTPS) open trong firewall
- [ ] Nginx/reverse proxy forward `/api/payos-webhook` đúng
- [ ] `.env` production có `PAYOS_WEBHOOK_URL` với domain
- [ ] PayOS dashboard configured với production URL
- [ ] Backend deployed và running
- [ ] Test payment → order update successful
- [ ] Monitoring/alerting setup (optional)

---

## 📊 EXPECTED BEHAVIOR

### ✅ Success Scenario

```
User pays on PayOS → Closes tab immediately
         ↓
PayOS Server → POST https://your-domain.com/api/payos-webhook
         ↓
Backend verifies signature → VALID
         ↓
Process webhook data → OrderCode: 123, Status: PAID
         ↓
Update Order 123 → Status: CONFIRMED
         ↓
Return 200 OK to PayOS
         ↓
User checks order later → Status: CONFIRMED ✅
```

### Logs hiển thị:

```
INFO - Received PayOS webhook
INFO - Webhook signature verification: VALID
INFO - Processing PayOS webhook - OrderCode: 123, Status: PAID, Amount: 50000
INFO - Updating order 123 to CONFIRMED via webhook
INFO - Order updated successfully
```

---

## ⚠️ COMMON ISSUES & SOLUTIONS

### 1. **401 Unauthorized / 403 Forbidden**

**Nguyên nhân:** Security config chưa permit webhook endpoint  
**Giải pháp:** ✅ Đã fix trong SecurityConfig.java

### 2. **Signature Verification Failed**

**Nguyên nhân:** PAYOS_CHECKSUM_KEY sai  
**Giải pháp:**

```bash
# Verify key trong .env match với PayOS dashboard
grep PAYOS_CHECKSUM_KEY .env
# Compare với key trên https://my.payos.vn/
```

### 3. **Webhook không được gọi**

**Nguyên nhân:** PayOS không thể reach webhook URL  
**Giải pháp:**

- Local: Dùng ngrok (localhost không thể reach từ internet)
- Production: Check SSL, firewall, domain resolution

### 4. **ngrok URL thay đổi mỗi lần restart**

**Giải pháp:**

```bash
# Dùng ngrok account để có fixed domain
ngrok config add-authtoken YOUR_TOKEN
ngrok http 8080 --domain=your-subdomain.ngrok-free.app
```

### 5. **Order không update**

**Kiểm tra:**

```bash
# Check logs chi tiết
tail -f logs/application.log | grep -A 5 webhook

# Check database
docker-compose exec mariadb mysql -u root -p -e \
  "SELECT id, status, updated_at FROM db_movie_ticket_booking.orders ORDER BY id DESC LIMIT 5;"
```

---

## 🎓 TECHNICAL DETAILS

### Webhook Flow

```
PayOS Server
    ↓ POST /api/payos-webhook
    ↓ Headers: x-payos-signature
    ↓ Body: {"code":"00","desc":"success","data":{...}}
    ↓
SecurityConfig → permitAll() (no JWT required)
    ↓
OrderController.handlePayOSWebhook()
    ↓
PayOSService.verifyWebhookSignature() → HMAC-SHA256
    ↓ (if valid)
PayOSService.processWebhook() → Parse JSON
    ↓
OrderService.updateOrderStatus(orderCode, CONFIRMED)
    ↓
Return 200 OK to PayOS
```

### Security

- ✅ Signature verification với HMAC-SHA256
- ✅ Sử dụng PAYOS_CHECKSUM_KEY làm secret
- ✅ HTTPS required (PayOS không gửi đến HTTP)
- ✅ Request body validation
- ✅ Full audit logging

---

## 📚 DOCUMENTATION LINKS

1. **Quick Start (5 phút):**
   `docs/PAYOS-WEBHOOK-QUICKSTART.md`

2. **Chi tiết Setup:**
   `docs/PAYOS-WEBHOOK-SETUP.md`

3. **Technical Changes:**
   `docs/PAYOS-WEBHOOK-CHANGES.md`

4. **Implementation Summary:**
   `PAYOS-WEBHOOK-IMPLEMENTATION.md`

---

## 🎉 SUMMARY

**Trạng thái hiện tại:** ✅ SẴN SÀNG TEST

**Những gì đã làm:**

1. ✅ Implement webhook processing trong backend
2. ✅ Add security configuration cho webhook endpoint
3. ✅ Setup environment variables
4. ✅ Tạo documentation đầy đủ
5. ✅ **FIX:** SecurityConfig permitAll webhook endpoint
6. ✅ **FIX:** Add PAYOS_WEBHOOK_URL vào .env

**Bước tiếp theo:**

1. 🧪 **TEST LOCAL** với ngrok (theo BƯỚC 1 ở trên)
2. 🚀 **DEPLOY PRODUCTION** (theo BƯỚC 2 ở trên)
3. ✅ **VERIFY** payment flow hoạt động với scenario đóng tab

**Thời gian ước tính:**

- Local testing: 15-30 phút
- Production deployment: 30-60 phút (tùy thuộc SSL setup)

---

**Good luck! 🚀**

Nếu gặp vấn đề, check logs và xem lại troubleshooting section trong docs.
