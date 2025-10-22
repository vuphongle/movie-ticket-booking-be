# 🚀 HƯỚNG DẪN NHANH - Thêm api.gocinema.io.vn vào SSL

## ✅ CÁCH ĐƠN GIẢN NHẤT (Copy & Paste)

### **BƯỚC 1: Thêm DNS Record** (2 phút)

Vào DNS provider của bạn (Cloudflare/GoDaddy/etc) và thêm:

```
Type: A
Name: api
Value: 159.223.38.127
TTL: Auto (hoặc 300)
```

**Kiểm tra:** Sau 2-5 phút, test:

```bash
dig +short api.gocinema.io.vn
# Phải trả về: 159.223.38.127
```

---

### **BƯỚC 2: SSH và chạy script** (3 phút)

```bash
# 1. SSH vào VPS
ssh root@159.223.38.127

# 2. Copy-paste toàn bộ đoạn này vào terminal:
cat > /tmp/fix-ssl.sh << 'SCRIPT_END'
#!/bin/bash
set -e

echo "🔐 Adding api.gocinema.io.vn to SSL..."

# Check DNS
API_IP=$(dig +short api.gocinema.io.vn | tail -n1)
if [ "$API_IP" != "159.223.38.127" ]; then
    echo "❌ DNS chưa đúng! Hiện tại: $API_IP"
    echo "Cần: 159.223.38.127"
    exit 1
fi
echo "✓ DNS OK"

# Install certbot nếu cần
if ! command -v certbot &> /dev/null; then
    apt update && apt install -y certbot python3-certbot-nginx
fi

# Create Nginx config
cat > /etc/nginx/sites-available/api.gocinema.io.vn << 'EOF'
server {
    listen 80;
    server_name api.gocinema.io.vn;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.gocinema.io.vn;

    ssl_certificate /etc/letsencrypt/live/gocinema.io.vn/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/gocinema.io.vn/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header x-payos-signature $http_x_payos_signature;
    }

    location / {
        return 200 'API Server';
        add_header Content-Type text/plain;
    }
}
EOF

ln -sf /etc/nginx/sites-available/api.gocinema.io.vn /etc/nginx/sites-enabled/
nginx -t

# Expand SSL
certbot --nginx \
    -d gocinema.io.vn \
    -d www.gocinema.io.vn \
    -d api.gocinema.io.vn \
    --expand \
    --non-interactive \
    --agree-tos \
    --email contact@gocinema.io.vn \
    --redirect

systemctl reload nginx

echo ""
echo "🎉 SUCCESS!"
echo ""
echo "Test: curl -I https://api.gocinema.io.vn/api/payos-webhook"
SCRIPT_END

# 3. Chạy script
bash /tmp/fix-ssl.sh
```

**Chờ 1-2 phút để script hoàn thành!**

---

### **BƯỚC 3: Verify thành công** (1 phút)

```bash
# Vẫn trong SSH, test:
curl -I https://api.gocinema.io.vn/api/payos-webhook

# Expected output:
# HTTP/2 200 (hoặc 405) ← OK!
# Không có SSL errors ← OK!
```

---

### **BƯỚC 4: Update backend config** (2 phút)

```bash
# 1. Tìm thư mục backend
cd ~
find . -name "docker-compose.yml" -type f 2>/dev/null | grep movie

# Hoặc nếu biết đường dẫn:
cd /root/movie-ticket-booking-be  # (hoặc đường dẫn của bạn)

# 2. Update .env
nano .env

# 3. Tìm dòng PAYOS_WEBHOOK_URL và sửa thành:
PAYOS_WEBHOOK_URL=https://api.gocinema.io.vn/api/payos-webhook

# 4. Save: Ctrl+X, Y, Enter

# 5. Restart backend
docker-compose restart backend

# 6. Check logs
docker-compose logs -f backend | grep -i "payos\|webhook"
# Ctrl+C để thoát
```

---

### **BƯỚC 5: Update PayOS Dashboard** (2 phút)

1. Mở browser: https://my.payos.vn/
2. Login
3. Vào **Settings** → **Webhook Configuration**
4. Nhập: `https://api.gocinema.io.vn/api/payos-webhook`
5. Click **Test** (nếu có)
6. **Save**

---

### **BƯỚC 6: Test payment flow** 🎉

1. Vào website của bạn
2. Tạo order mới
3. Chọn thanh toán PayOS
4. Thanh toán thành công
5. **ĐÓNG TAB NGAY** (test webhook hoạt động)
6. Check order status → Phải là **CONFIRMED**

---

## 🔍 Troubleshooting

### Lỗi 1: DNS chưa resolve

```bash
# Check DNS
dig +short api.gocinema.io.vn

# Nếu không trả về 159.223.38.127:
# - Kiểm tra lại DNS record
# - Đợi thêm 5-10 phút
# - Clear DNS cache: sudo systemd-resolve --flush-caches
```

### Lỗi 2: Certbot fails

```bash
# Check ports
sudo netstat -tlnp | grep -E ':80|:443'

# Check firewall
sudo ufw status

# Nếu cần mở ports:
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw reload
```

### Lỗi 3: Backend không nhận webhook

```bash
# Check backend running
docker-compose ps

# Check port 8080
sudo netstat -tlnp | grep :8080

# Check logs
docker-compose logs backend | tail -50

# Restart backend
docker-compose restart backend
```

---

## 📋 Checklist

- [ ] DNS A record added (api → 159.223.38.127)
- [ ] DNS resolving correctly (dig test)
- [ ] Script chạy thành công
- [ ] SSL certificate expanded
- [ ] curl test HTTPS works
- [ ] Backend .env updated
- [ ] Backend restarted
- [ ] PayOS dashboard configured
- [ ] Test payment successful

---

## 🆘 Cần giúp?

Nếu script fail, gửi cho tôi output của:

```bash
# Check DNS
dig +short api.gocinema.io.vn

# Check SSL
sudo openssl x509 -in /etc/letsencrypt/live/gocinema.io.vn/fullchain.pem -text -noout | grep DNS:

# Check Nginx
sudo nginx -t

# Test endpoint
curl -v https://api.gocinema.io.vn/api/payos-webhook 2>&1 | head -30
```

---

**Tổng thời gian: 10-15 phút** ⏱️

Good luck! 🚀
