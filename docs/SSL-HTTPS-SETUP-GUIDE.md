# 🔒 SSL/HTTPS Setup Guide cho VPS

## 📋 Thông tin VPS hiện tại

- **IP**: 159.223.38.127
- **Domains**:
  - `gocinema.io.vn` - User Frontend
  - `admin.gocinema.io.vn` - Admin Frontend
  - Backend đang chạy trên port 8080 (cần thêm subdomain)

## 🎯 Mục tiêu

Cài đặt SSL certificate (Let's Encrypt) cho:

1. ✅ `gocinema.io.vn` (User Frontend)
2. ✅ `admin.gocinema.io.vn` (Admin)
3. ✅ `api.gocinema.io.vn` (Backend API) - **CẦN THIẾT CHO PAYOS WEBHOOK**

## 📝 Yêu cầu trước khi bắt đầu

### 1. DNS Configuration

Đảm bảo DNS records đã trỏ đúng:

```
A     gocinema.io.vn          → 159.223.38.127
A     www.gocinema.io.vn      → 159.223.38.127
A     admin.gocinema.io.vn    → 159.223.38.127
A     api.gocinema.io.vn      → 159.223.38.127  ⚠️ PHẢI TẠO MỚI
```

**Kiểm tra DNS**:

```bash
# Test từ máy local
dig gocinema.io.vn +short
dig admin.gocinema.io.vn +short
dig api.gocinema.io.vn +short

# Phải trả về: 159.223.38.127
```

### 2. Port 443 phải mở

```bash
# Kiểm tra trên VPS
sudo ufw status

# Nếu chưa mở port 443
sudo ufw allow 443/tcp
sudo ufw status
```

## 🚀 Phương án triển khai

### Phương án 1: Sử dụng Certbot trong Container (RECOMMENDED)

Dùng container certbot để tự động renew SSL.

### Phương án 2: Cài Certbot trực tiếp trên VPS

Dùng certbot system-wide.

---

## 📦 Phương án 1: Certbot Container (RECOMMENDED)

### Bước 1: Tạo Nginx config mới với SSL

Tạo file config mới trên VPS:

```bash
ssh root@159.223.38.127

# Tạo thư mục cho SSL certificates
mkdir -p /etc/nginx-ssl/letsencrypt
mkdir -p /etc/nginx-ssl/conf.d

# Tạo nginx config mới
cat > /etc/nginx-ssl/nginx.conf << 'EOF'
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet/stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;

    # SSL Configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Compression
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml text/javascript
               application/json application/javascript application/xml+rss
               application/rss+xml font/truetype font/opentype
               application/vnd.ms-fontobject image/svg+xml;

    # HTTP to HTTPS redirect for User Frontend
    server {
        listen 80;
        server_name gocinema.io.vn www.gocinema.io.vn;

        # Allow Let's Encrypt validation
        location /.well-known/acme-challenge/ {
            root /var/www/certbot;
        }

        location / {
            return 301 https://$server_name$request_uri;
        }
    }

    # HTTPS User Frontend
    server {
        listen 443 ssl http2;
        server_name gocinema.io.vn www.gocinema.io.vn;

        ssl_certificate /etc/letsencrypt/live/gocinema.io.vn/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/gocinema.io.vn/privkey.pem;

        access_log /var/log/nginx/gocinema-user-access.log main;
        error_log /var/log/nginx/gocinema-user-error.log warn;

        # Security headers
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-XSS-Protection "1; mode=block" always;

        location / {
            proxy_pass http://movie-booking-frontend-user:80;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection 'upgrade';
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_cache_bypass $http_upgrade;
        }

        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
    }

    # HTTP to HTTPS redirect for Admin
    server {
        listen 80;
        server_name admin.gocinema.io.vn;

        location /.well-known/acme-challenge/ {
            root /var/www/certbot;
        }

        location / {
            return 301 https://$server_name$request_uri;
        }
    }

    # HTTPS Admin Frontend
    server {
        listen 443 ssl http2;
        server_name admin.gocinema.io.vn;

        ssl_certificate /etc/letsencrypt/live/admin.gocinema.io.vn/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/admin.gocinema.io.vn/privkey.pem;

        access_log /var/log/nginx/gocinema-admin-access.log main;
        error_log /var/log/nginx/gocinema-admin-error.log warn;

        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-XSS-Protection "1; mode=block" always;

        location / {
            proxy_pass http://movie-booking-frontend-admin:80;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection 'upgrade';
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_cache_bypass $http_upgrade;
        }

        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
    }

    # HTTP to HTTPS redirect for API
    server {
        listen 80;
        server_name api.gocinema.io.vn;

        location /.well-known/acme-challenge/ {
            root /var/www/certbot;
        }

        location / {
            return 301 https://$server_name$request_uri;
        }
    }

    # HTTPS Backend API - QUAN TRỌNG CHO PAYOS WEBHOOK
    server {
        listen 443 ssl http2;
        server_name api.gocinema.io.vn;

        ssl_certificate /etc/letsencrypt/live/api.gocinema.io.vn/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/api.gocinema.io.vn/privkey.pem;

        access_log /var/log/nginx/gocinema-api-access.log main;
        error_log /var/log/nginx/gocinema-api-error.log warn;

        add_header Strict-Transport-Security "max-age=31536000" always;
        add_header X-Content-Type-Options "nosniff" always;

        # Backend API
        location /api/ {
            proxy_pass http://movie-booking-backend:8080/api/;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;

            # Important for PayOS webhook
            proxy_set_header x-payos-signature $http_x_payos_signature;

            # Timeouts
            proxy_connect_timeout 60s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
        }

        location /health {
            access_log off;
            return 200 "api healthy\n";
            add_header Content-Type text/plain;
        }
    }

    # Default server
    server {
        listen 80 default_server;
        listen 443 ssl http2 default_server;
        server_name _;

        ssl_certificate /etc/letsencrypt/live/gocinema.io.vn/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/gocinema.io.vn/privkey.pem;

        location / {
            return 444;
        }
    }
}
EOF
```

### Bước 2: Tạo docker-compose cho SSL setup

```bash
cat > /root/docker-compose-ssl.yml << 'EOF'
version: '3.8'

services:
  nginx-ssl:
    image: nginx:alpine
    container_name: gocinema-nginx-ssl
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /etc/nginx-ssl/nginx.conf:/etc/nginx/nginx.conf:ro
      - /etc/nginx-ssl/letsencrypt:/etc/letsencrypt:ro
      - /var/www/certbot:/var/www/certbot:ro
      - nginx-logs:/var/log/nginx
    networks:
      - gocinema-network
    depends_on:
      - certbot

  certbot:
    image: certbot/certbot:latest
    container_name: gocinema-certbot
    volumes:
      - /etc/nginx-ssl/letsencrypt:/etc/letsencrypt
      - /var/www/certbot:/var/www/certbot
    entrypoint: "/bin/sh -c 'trap exit TERM; while :; do certbot renew; sleep 12h & wait $${!}; done;'"

networks:
  gocinema-network:
    external: true

volumes:
  nginx-logs:
EOF
```

### Bước 3: Obtain SSL Certificates

**Quan trọng**: Tạm thời stop nginx container hiện tại để certbot có thể bind port 80:

```bash
# Stop nginx hiện tại
docker stop gocinema-nginx-proxy

# Verify DNS trỏ đúng
dig gocinema.io.vn +short
dig admin.gocinema.io.vn +short
dig api.gocinema.io.vn +short
# Phải trả về: 159.223.38.127

# Obtain certificates (chạy lần lượt cho từng domain)
docker run -it --rm \
  -v /etc/nginx-ssl/letsencrypt:/etc/letsencrypt \
  -v /var/www/certbot:/var/www/certbot \
  -p 80:80 \
  certbot/certbot certonly \
  --standalone \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d gocinema.io.vn -d www.gocinema.io.vn

docker run -it --rm \
  -v /etc/nginx-ssl/letsencrypt:/etc/letsencrypt \
  -v /var/www/certbot:/var/www/certbot \
  -p 80:80 \
  certbot/certbot certonly \
  --standalone \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d admin.gocinema.io.vn

docker run -it --rm \
  -v /etc/nginx-ssl/letsencrypt:/etc/letsencrypt \
  -v /var/www/certbot:/var/www/certbot \
  -p 80:80 \
  certbot/certbot certonly \
  --standalone \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d api.gocinema.io.vn
```

### Bước 4: Start Nginx với SSL

```bash
# Remove old nginx container
docker rm gocinema-nginx-proxy

# Start new SSL-enabled nginx
docker-compose -f /root/docker-compose-ssl.yml up -d

# Verify
docker ps | grep nginx
docker logs gocinema-nginx-ssl
```

### Bước 5: Test SSL

```bash
# Test từ VPS
curl -I https://gocinema.io.vn
curl -I https://admin.gocinema.io.vn
curl -I https://api.gocinema.io.vn/health

# Test từ máy local
curl -I https://gocinema.io.vn
curl -I https://api.gocinema.io.vn/api/orders
```

### Bước 6: Configure Auto-renewal

Certificates sẽ tự động renew mỗi 12h nhờ certbot container.

Kiểm tra renewal:

```bash
docker exec gocinema-certbot certbot renew --dry-run
```

---

## 📦 Phương án 2: Cài Certbot trực tiếp trên VPS

### Bước 1: Cài đặt Certbot

```bash
ssh root@159.223.38.127

# Ubuntu/Debian
apt update
apt install -y certbot

# Verify
certbot --version
```

### Bước 2: Stop Nginx container

```bash
docker stop gocinema-nginx-proxy
```

### Bước 3: Obtain certificates

```bash
# Verify DNS
dig gocinema.io.vn +short
dig admin.gocinema.io.vn +short
dig api.gocinema.io.vn +short

# Get certificates
certbot certonly --standalone \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d gocinema.io.vn -d www.gocinema.io.vn

certbot certonly --standalone \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d admin.gocinema.io.vn

certbot certonly --standalone \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d api.gocinema.io.vn

# Certificates sẽ được lưu tại: /etc/letsencrypt/live/
```

### Bước 4: Update Nginx container với SSL

Tạo nginx config mới và mount certificates:

```bash
# Copy config mẫu từ Phương án 1
# Sau đó start nginx container với volumes:

docker run -d \
  --name gocinema-nginx-ssl \
  --restart unless-stopped \
  --network gocinema-network \
  -p 80:80 \
  -p 443:443 \
  -v /path/to/nginx.conf:/etc/nginx/nginx.conf:ro \
  -v /etc/letsencrypt:/etc/letsencrypt:ro \
  nginx:alpine
```

### Bước 5: Setup Auto-renewal

```bash
# Add cron job
crontab -e

# Add this line (renew twice daily)
0 0,12 * * * certbot renew --quiet && docker exec gocinema-nginx-ssl nginx -s reload
```

---

## ✅ Verification Checklist

Sau khi setup SSL:

- [ ] `https://gocinema.io.vn` - Load và có padlock icon
- [ ] `https://admin.gocinema.io.vn` - Load và có padlock icon
- [ ] `https://api.gocinema.io.vn/health` - Return "api healthy"
- [ ] HTTP redirect to HTTPS (test `http://gocinema.io.vn`)
- [ ] SSL Labs test: https://www.ssllabs.com/ssltest/
- [ ] Check certificate expiry: `openssl s_client -connect gocinema.io.vn:443 -servername gocinema.io.vn | grep 'Not After'`

## 🔧 Configure PayOS Webhook

Sau khi có SSL, update webhook URL:

### 1. Update Backend Environment

```bash
# SSH vào VPS
ssh root@159.223.38.127

# Update backend container environment
docker exec -it movie-booking-backend bash
# Hoặc rebuild với env mới

# Set webhook URL
export PAYOS_WEBHOOK_URL=https://api.gocinema.io.vn/api/payos-webhook
```

### 2. Configure trên PayOS Dashboard

1. Login: https://my.payos.vn/
2. Settings → Webhook URL
3. Nhập: `https://api.gocinema.io.vn/api/payos-webhook`
4. Save

### 3. Test Webhook

```bash
# Test từ local
curl -X POST https://api.gocinema.io.vn/api/payos-webhook \
  -H "Content-Type: application/json" \
  -H "x-payos-signature: test" \
  -d '{"code":"00","data":{"orderCode":123,"status":"PAID"}}'

# Check logs
ssh root@159.223.38.127
docker logs movie-booking-backend | grep webhook
```

## 🚨 Troubleshooting

### Issue: DNS not propagating

```bash
# Wait 5-10 minutes for DNS propagation
# Check with:
dig api.gocinema.io.vn +short

# Flush DNS cache (local machine)
# macOS: sudo dscacheutil -flushcache
# Windows: ipconfig /flushdns
# Linux: sudo systemd-resolve --flush-caches
```

### Issue: Port 443 blocked

```bash
# Check firewall
sudo ufw status
sudo ufw allow 443/tcp

# Check if port is listening
sudo netstat -tlnp | grep :443
```

### Issue: Certificate verification failed

```bash
# Check cert files
ls -la /etc/letsencrypt/live/gocinema.io.vn/

# Test certificate
openssl x509 -in /etc/letsencrypt/live/gocinema.io.vn/fullchain.pem -text -noout
```

### Issue: Nginx won't start

```bash
# Check config syntax
docker run --rm -v /etc/nginx-ssl/nginx.conf:/etc/nginx/nginx.conf:ro nginx:alpine nginx -t

# Check logs
docker logs gocinema-nginx-ssl
```

## 📚 Resources

- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [Certbot Documentation](https://certbot.eff.org/docs/)
- [Nginx SSL Configuration](https://nginx.org/en/docs/http/configuring_https_servers.html)
- [SSL Labs Test](https://www.ssllabs.com/ssltest/)

---

**Next Steps**: Sau khi có SSL, nhớ update PayOS webhook URL và test thoroughly!
