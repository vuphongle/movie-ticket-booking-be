# 🚀 SSL Setup Quick Start - 5 Phút Setup HTTPS

## ⚡ Cách nhanh nhất (Recommended)

### Bước 1: Cấu hình DNS (LÀM TRƯỚC)

Truy cập DNS provider của bạn và tạo A records:

```
A     api.gocinema.io.vn    → 159.223.38.127
```

Kiểm tra DNS đã propagate:

```bash
dig api.gocinema.io.vn +short
# Phải trả về: 159.223.38.127
```

⏰ **Đợi 5-10 phút** nếu DNS chưa propagate

### Bước 2: Upload và chạy script

```bash
# 1. Upload script lên VPS
scp scripts/setup-ssl-vps.sh root@159.223.38.127:/root/

# 2. SSH vào VPS
ssh root@159.223.38.127

# 3. Chạy script
chmod +x /root/setup-ssl-vps.sh
/root/setup-ssl-vps.sh your-email@example.com

# Script sẽ tự động:
# - Kiểm tra DNS
# - Cài certbot
# - Obtain SSL certificates
# - Configure nginx với SSL
# - Setup auto-renewal
# - Test SSL
```

### Bước 3: Verify

```bash
# Test từ local machine
curl -I https://gocinema.io.vn
curl -I https://admin.gocinema.io.vn
curl -I https://api.gocinema.io.vn/health

# Mở browser:
# https://gocinema.io.vn
# https://admin.gocinema.io.vn
# https://api.gocinema.io.vn/health
```

### Bước 4: Update PayOS Webhook

```bash
# 1. Update backend environment variable
ssh root@159.223.38.127

# Find your backend container
docker ps | grep backend

# Recreate với env mới hoặc update docker-compose
# Thêm vào .env hoặc docker-compose.yml:
PAYOS_WEBHOOK_URL=https://api.gocinema.io.vn/api/payos-webhook

# Restart backend
docker restart movie-booking-backend

# 2. Configure trên PayOS Dashboard
# - Login: https://my.payos.vn/
# - Settings → Webhook
# - URL: https://api.gocinema.io.vn/api/payos-webhook
# - Save
```

### Bước 5: Test Webhook

```bash
# Test endpoint
curl -X POST https://api.gocinema.io.vn/api/payos-webhook \
  -H "Content-Type: application/json" \
  -H "x-payos-signature: test" \
  -d '{"code":"00","data":{"orderCode":123,"status":"PAID"}}'

# Check logs
docker logs movie-booking-backend | grep webhook
```

---

## 🎉 DONE!

Bạn đã có:

- ✅ HTTPS cho tất cả domains
- ✅ SSL certificates tự động renew
- ✅ Backend API có HTTPS endpoint
- ✅ Ready cho PayOS webhook

---

## 🔧 Manual Setup (Nếu script không chạy)

### 1. Cài Certbot

```bash
ssh root@159.223.38.127

apt update
apt install -y certbot
```

### 2. Stop Nginx

```bash
docker stop gocinema-nginx-proxy
```

### 3. Get Certificates

```bash
certbot certonly --standalone \
  --email your-email@example.com \
  --agree-tos \
  -d gocinema.io.vn -d www.gocinema.io.vn

certbot certonly --standalone \
  --email your-email@example.com \
  --agree-tos \
  -d admin.gocinema.io.vn

certbot certonly --standalone \
  --email your-email@example.com \
  --agree-tos \
  -d api.gocinema.io.vn
```

### 4. Download nginx.conf

Từ máy local:

```bash
scp docs/ssl-nginx.conf root@159.223.38.127:/etc/nginx-ssl/nginx.conf
```

Hoặc copy nội dung từ [SSL-HTTPS-SETUP-GUIDE.md](./SSL-HTTPS-SETUP-GUIDE.md)

### 5. Start Nginx với SSL

```bash
docker rm -f gocinema-nginx-proxy

docker run -d \
  --name gocinema-nginx-ssl \
  --restart unless-stopped \
  --network gocinema-network \
  -p 80:80 \
  -p 443:443 \
  -v /etc/nginx-ssl/nginx.conf:/etc/nginx/nginx.conf:ro \
  -v /etc/letsencrypt:/etc/letsencrypt:ro \
  nginx:alpine

# Check logs
docker logs gocinema-nginx-ssl
```

### 6. Setup Auto-renewal

```bash
cat > /root/renew-certs.sh << 'EOF'
#!/bin/bash
certbot renew --quiet
docker exec gocinema-nginx-ssl nginx -s reload
EOF

chmod +x /root/renew-certs.sh

# Add to crontab
crontab -e
# Add: 0 0,12 * * * /root/renew-certs.sh >> /var/log/cert-renewal.log 2>&1
```

---

## ❓ Troubleshooting

### DNS không resolve

```bash
# Kiểm tra
dig api.gocinema.io.vn +short

# Nếu không trả về IP đúng:
# - Đợi DNS propagate (5-10 phút)
# - Kiểm tra DNS provider
# - Flush DNS local: sudo dscacheutil -flushcache (macOS)
```

### Certbot failed

```bash
# Check port 80
sudo netstat -tlnp | grep :80

# Stop services using port 80
docker stop gocinema-nginx-proxy

# Try again
certbot certonly --standalone -d api.gocinema.io.vn
```

### Nginx không start

```bash
# Check config
docker run --rm \
  -v /etc/nginx-ssl/nginx.conf:/etc/nginx/nginx.conf:ro \
  nginx:alpine nginx -t

# Check logs
docker logs gocinema-nginx-ssl

# Check certificates
ls -la /etc/letsencrypt/live/
```

### SSL not working

```bash
# Test local
curl -Ik https://api.gocinema.io.vn

# Check certificate
openssl s_client -connect api.gocinema.io.vn:443 -servername api.gocinema.io.vn

# Check firewall
sudo ufw status
sudo ufw allow 443/tcp
```

---

## 📞 Need Help?

1. Check full guide: [SSL-HTTPS-SETUP-GUIDE.md](./SSL-HTTPS-SETUP-GUIDE.md)
2. Check logs: `docker logs gocinema-nginx-ssl`
3. Test SSL: https://www.ssllabs.com/ssltest/
4. Verify DNS: `dig api.gocinema.io.vn +short`

---

**Estimated Time**: 5-10 minutes (excluding DNS propagation)
**Difficulty**: Easy with script, Medium manual
