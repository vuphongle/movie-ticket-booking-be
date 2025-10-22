# 🔐 Hướng dẫn thêm api.gocinema.io.vn vào SSL Certificate

## 📋 Chuẩn bị

### 1. Thêm DNS Record (QUAN TRỌNG!)

Trước khi chạy script, bạn PHẢI thêm DNS A record:

**Vào DNS provider của bạn (nơi quản lý domain gocinema.io.vn):**

```
Type: A
Name: api
Value: 159.223.38.127
TTL: 300 (hoặc Auto)
```

Sau khi thêm, đợi 1-5 phút để DNS propagate.

**Verify DNS:**
```bash
# Trên máy local
dig +short api.gocinema.io.vn

# Phải trả về: 159.223.38.127
```

---

## 🚀 Các bước thực hiện

### Bước 1: SSH vào VPS

```bash
ssh root@159.223.38.127
```

### Bước 2: Backup cấu hình hiện tại (Safety first!)

```bash
# Backup Nginx configs
sudo cp -r /etc/nginx /etc/nginx.backup.$(date +%Y%m%d)

# Backup SSL certs info
sudo ls -la /etc/letsencrypt/live/
```

### Bước 3: Download và chạy script

**Option A: Sử dụng script tự động (RECOMMENDED)**

```bash
# Download script
cd /tmp
cat > add-api-ssl.sh << 'EOF'
#!/bin/bash

# Script to add api.gocinema.io.vn to SSL certificate
set -e

DOMAIN="gocinema.io.vn"
API_DOMAIN="api.gocinema.io.vn"
WWW_DOMAIN="www.gocinema.io.vn"

echo "🔐 Adding api.gocinema.io.vn to SSL Certificate"
echo "=============================================="

# Check DNS
echo "Checking DNS..."
API_IP=$(dig +short ${API_DOMAIN} | tail -n1)
SERVER_IP=$(curl -s ifconfig.me)

echo "API Domain resolves to: ${API_IP}"
echo "This server IP: ${SERVER_IP}"

if [ "${API_IP}" != "${SERVER_IP}" ]; then
    echo "⚠️  WARNING: DNS not configured correctly!"
    echo "Please add DNS A record: api -> ${SERVER_IP}"
    exit 1
fi

# Install certbot if needed
if ! command -v certbot &> /dev/null; then
    echo "Installing certbot..."
    apt update
    apt install -y certbot python3-certbot-nginx
fi

# Create Nginx config for API subdomain
cat > /etc/nginx/sites-available/${API_DOMAIN} << 'NGINX_CONFIG'
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
        return 200 'API Server Running';
        add_header Content-Type text/plain;
    }
}
NGINX_CONFIG

# Enable site
ln -sf /etc/nginx/sites-available/${API_DOMAIN} /etc/nginx/sites-enabled/

# Test Nginx
nginx -t

# Expand SSL certificate
echo "Requesting SSL certificate..."
certbot --nginx \
    -d ${DOMAIN} \
    -d ${WWW_DOMAIN} \
    -d ${API_DOMAIN} \
    --expand \
    --non-interactive \
    --agree-tos \
    --redirect

# Reload Nginx
systemctl reload nginx

echo ""
echo "✅ SUCCESS!"
echo "Test: curl -I https://api.gocinema.io.vn/api/payos-webhook"
echo ""

EOF

# Make executable
chmod +x add-api-ssl.sh

# Run it
sudo ./add-api-ssl.sh
```

**Option B: Manual (nếu script fail)**

```bash
# 1. Create Nginx config
sudo nano /etc/nginx/sites-available/api.gocinema.io.vn

# Paste nội dung (xem phần Config mẫu ở dưới)

# 2. Enable site
sudo ln -s /etc/nginx/sites-available/api.gocinema.io.vn /etc/nginx/sites-enabled/

# 3. Test Nginx
sudo nginx -t

# 4. Expand SSL certificate
sudo certbot --nginx \
    -d gocinema.io.vn \
    -d www.gocinema.io.vn \
    -d api.gocinema.io.vn \
    --expand

# 5. Reload Nginx
sudo systemctl reload nginx
```

---

## ✅ Verify thành công

### 1. Check SSL Certificate

```bash
# Xem domains trong cert
sudo openssl x509 -in /etc/letsencrypt/live/gocinema.io.vn/fullchain.pem -text -noout | grep DNS:

# Phải thấy:
# DNS:gocinema.io.vn, DNS:www.gocinema.io.vn, DNS:api.gocinema.io.vn
```

### 2. Test HTTPS endpoint

```bash
# From VPS
curl -I https://api.gocinema.io.vn/api/payos-webhook

# Expected: 200 OK hoặc 405 Method Not Allowed (OK)
# Should NOT see SSL errors
```

### 3. Test from external

```bash
# From your local machine
curl -I https://api.gocinema.io.vn/api/payos-webhook

# Should work without SSL errors
```

---

## 🔧 Update Backend Configuration

### 1. Update .env trên VPS

```bash
# SSH to VPS
ssh root@159.223.38.127

# Navigate to backend
cd /path/to/movie-ticket-booking-be

# Update .env
nano .env

# Change line:
PAYOS_WEBHOOK_URL=https://api.gocinema.io.vn/api/payos-webhook

# Save (Ctrl+X, Y, Enter)
```

### 2. Restart Backend

```bash
# If using Docker
docker-compose restart backend

# If using systemd
sudo systemctl restart movie-booking-backend

# Check logs
docker-compose logs -f backend | grep webhook
```

---

## 🎯 Update PayOS Dashboard

1. Login: https://my.payos.vn/
2. Go to **Settings** → **Webhook Configuration**
3. Enter URL: `https://api.gocinema.io.vn/api/payos-webhook`
4. Click **Test** (if available)
5. **Save**

---

## 🧪 Test Payment Flow

```bash
# Monitor logs
docker-compose logs -f backend | grep webhook

# Then:
# 1. Create order on frontend
# 2. Pay via PayOS
# 3. Close tab immediately
# 4. Check logs for webhook received
# 5. Verify order status updated
```

---

## 📦 Config mẫu cho Nginx

```nginx
# /etc/nginx/sites-available/api.gocinema.io.vn

server {
    listen 80;
    server_name api.gocinema.io.vn;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.gocinema.io.vn;

    # SSL (certbot will update these paths)
    ssl_certificate /etc/letsencrypt/live/gocinema.io.vn/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/gocinema.io.vn/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # API Backend
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Important for PayOS webhook signature
        proxy_set_header x-payos-signature $http_x_payos_signature;
        
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Health check
    location /actuator/health {
        proxy_pass http://localhost:8080/actuator/health;
        access_log off;
    }

    location / {
        return 200 'API Server Running';
        add_header Content-Type text/plain;
    }
}
```

---

## ⚠️ Troubleshooting

### Issue 1: DNS not resolving

```bash
# Check DNS
dig +short api.gocinema.io.vn

# If empty or wrong IP, add DNS A record
# Wait 5-10 minutes and try again
```

### Issue 2: Certbot fails

```bash
# Check Nginx is running
sudo systemctl status nginx

# Check port 80 is accessible
sudo netstat -tlnp | grep :80

# Check firewall
sudo ufw status

# Try manual mode
sudo certbot certonly --manual -d api.gocinema.io.vn
```

### Issue 3: 502 Bad Gateway

```bash
# Check backend is running
sudo systemctl status movie-booking-backend
# or
docker-compose ps

# Check backend port
sudo netstat -tlnp | grep :8080

# Check Nginx logs
sudo tail -f /var/log/nginx/error.log
```

### Issue 4: Webhook still fails

```bash
# Test endpoint directly
curl -v https://api.gocinema.io.vn/api/payos-webhook

# Check SSL
echo | openssl s_client -showcerts -servername api.gocinema.io.vn -connect api.gocinema.io.vn:443 2>/dev/null | grep -A 10 "Certificate chain"

# Check backend logs
docker-compose logs backend | tail -50
```

---

## 📚 Tham khảo thêm

- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [Certbot Nginx Plugin](https://certbot.eff.org/instructions?ws=nginx&os=ubuntufocal)
- [Nginx Proxy Configuration](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/)

---

**Good luck! 🚀**
