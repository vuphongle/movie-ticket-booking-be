#!/bin/bash

# Script to add api.gocinema.io.vn to existing Docker Nginx + SSL setup
set -e

echo "🔐 Adding api.gocinema.io.vn to SSL (Docker setup)"
echo "=================================================="
echo ""

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

NGINX_DIR="/opt/nginx-proxy"
NGINX_CONTAINER="gocinema-nginx-ssl"
DOMAIN="gocinema.io.vn"
API_DOMAIN="api.gocinema.io.vn"

# Check DNS
echo "Step 1/6: Checking DNS..."
API_IP=$(dig +short ${API_DOMAIN} | tail -n1)
SERVER_IP=$(curl -s ifconfig.me)

if [ "${API_IP}" != "${SERVER_IP}" ]; then
    echo -e "${YELLOW}⚠️  DNS: ${API_IP}, Server: ${SERVER_IP}${NC}"
    read -p "Continue? (yes/no): " cont
    [ "$cont" != "yes" ] && exit 1
fi
echo -e "${GREEN}✓ DNS OK${NC}"
echo ""

# Backup nginx config
echo "Step 2/6: Backing up nginx.conf..."
cp ${NGINX_DIR}/nginx.conf ${NGINX_DIR}/nginx.conf.backup.$(date +%Y%m%d_%H%M%S)
echo -e "${GREEN}✓ Backup created${NC}"
echo ""

# Add API server block to nginx.conf
echo "Step 3/6: Updating nginx.conf..."
cat > /tmp/api-server-block.txt << 'EOF'

    # API Backend - api.gocinema.io.vn
    server {
        listen 80;
        server_name api.gocinema.io.vn;

        access_log /var/log/nginx/gocinema-api-access.log main;
        error_log /var/log/nginx/gocinema-api-error.log warn;

        # Security headers
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-XSS-Protection "1; mode=block" always;

        # Proxy to Backend container
        location /api/ {
            proxy_pass http://movie-booking-backend:8080/api/;
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
        location /health {
            access_log off;
            return 200 "API healthy\n";
            add_header Content-Type text/plain;
        }
    }
EOF

# Insert API server block before the default server block
sed -i '/# Default server - reject unknown domains/r /tmp/api-server-block.txt' ${NGINX_DIR}/nginx.conf
echo -e "${GREEN}✓ nginx.conf updated${NC}"
echo ""

# Test nginx config
echo "Step 4/6: Testing nginx config..."
docker exec ${NGINX_CONTAINER} nginx -t
if [ $? -ne 0 ]; then
    echo "Nginx config test failed! Restoring backup..."
    cp ${NGINX_DIR}/nginx.conf.backup.* ${NGINX_DIR}/nginx.conf
    exit 1
fi
echo -e "${GREEN}✓ Config valid${NC}"
echo ""

# Reload nginx
echo "Step 5/6: Reloading nginx..."
docker exec ${NGINX_CONTAINER} nginx -s reload
echo -e "${GREEN}✓ Nginx reloaded${NC}"
echo ""

# Expand SSL certificate
echo "Step 6/6: Expanding SSL certificate..."
docker exec ${NGINX_CONTAINER} sh -c "
    apk add --no-cache certbot certbot-nginx 2>/dev/null || true
    certbot --nginx \
        -d ${DOMAIN} \
        -d www.${DOMAIN} \
        -d admin.${DOMAIN} \
        -d ${API_DOMAIN} \
        --expand \
        --non-interactive \
        --agree-tos \
        --email contact@${DOMAIN} \
        --redirect
"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ SSL certificate expanded!${NC}"
    docker exec ${NGINX_CONTAINER} nginx -s reload
else
    echo "SSL expansion failed. Checking if certbot is available..."
    echo "You may need to run certbot manually or use certbot container."
fi

echo ""
echo "=================================================="
echo -e "${GREEN}🎉 Configuration Updated!${NC}"
echo "=================================================="
echo ""
echo "Next steps:"
echo "1. Test: curl -I http://api.gocinema.io.vn/api/payos-webhook"
echo "2. If SSL works: curl -I https://api.gocinema.io.vn/api/payos-webhook"
echo "3. Update backend .env: PAYOS_WEBHOOK_URL=https://api.gocinema.io.vn/api/payos-webhook"
echo "4. Restart backend: cd /opt/movie-ticket-booking-be && docker-compose restart backend"
echo ""
