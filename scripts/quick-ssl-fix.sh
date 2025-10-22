#!/bin/bash

# Quick Script - Add api.gocinema.io.vn to SSL
# Run on VPS: bash <(curl -s THIS_URL)

set -e

echo "🔐 Quick SSL Fix for api.gocinema.io.vn"
echo "========================================"
echo ""

# Check if root
if [ "$EUID" -ne 0 ]; then 
    echo "Please run as root: sudo bash quick-ssl-fix.sh"
    exit 1
fi

DOMAIN="gocinema.io.vn"
API_DOMAIN="api.gocinema.io.vn"
WWW_DOMAIN="www.gocinema.io.vn"
SERVER_IP="159.223.38.127"

# Step 1: Check DNS
echo "Step 1/5: Checking DNS..."
API_IP=$(dig +short ${API_DOMAIN} 2>/dev/null | tail -n1)

if [ -z "$API_IP" ]; then
    echo ""
    echo "❌ DNS not configured!"
    echo ""
    echo "Please add this DNS A record first:"
    echo "  Type: A"
    echo "  Name: api"
    echo "  Value: ${SERVER_IP}"
    echo ""
    echo "After adding, wait 5 minutes and run this script again."
    exit 1
fi

if [ "${API_IP}" != "${SERVER_IP}" ]; then
    echo "⚠️  DNS points to ${API_IP}, should be ${SERVER_IP}"
    read -p "Continue anyway? (yes/no): " cont
    if [ "$cont" != "yes" ]; then
        exit 1
    fi
fi

echo "✓ DNS OK (${API_IP})"
echo ""

# Step 2: Install certbot
echo "Step 2/5: Checking certbot..."
if ! command -v certbot &> /dev/null; then
    echo "Installing certbot..."
    apt update -qq
    apt install -y certbot python3-certbot-nginx
fi
echo "✓ Certbot installed"
echo ""

# Step 3: Create Nginx config
echo "Step 3/5: Creating Nginx config..."
cat > /etc/nginx/sites-available/${API_DOMAIN} << 'EOF'
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

ln -sf /etc/nginx/sites-available/${API_DOMAIN} /etc/nginx/sites-enabled/
echo "✓ Nginx config created"
echo ""

# Step 4: Test and expand SSL
echo "Step 4/5: Testing Nginx..."
nginx -t
echo ""

echo "Step 5/5: Updating SSL certificate..."
echo "This will add api.gocinema.io.vn to your existing certificate..."
echo ""

certbot --nginx \
    -d ${DOMAIN} \
    -d ${WWW_DOMAIN} \
    -d ${API_DOMAIN} \
    --expand \
    --non-interactive \
    --agree-tos \
    --email contact@${DOMAIN} \
    --redirect \
    --quiet

echo ""
echo "✓ SSL certificate updated!"
echo ""

# Reload Nginx
systemctl reload nginx
echo "✓ Nginx reloaded"
echo ""

# Verify
echo "========================================"
echo "🎉 SUCCESS!"
echo "========================================"
echo ""
echo "Certificate now includes:"
openssl x509 -in /etc/letsencrypt/live/${DOMAIN}/fullchain.pem -text -noout | grep "DNS:" | sed 's/DNS://g'
echo ""
echo "✅ Test it:"
echo "   curl -I https://api.gocinema.io.vn/api/payos-webhook"
echo ""
echo "📝 Next steps:"
echo "   1. Update backend .env: PAYOS_WEBHOOK_URL=https://api.gocinema.io.vn/api/payos-webhook"
echo "   2. Restart backend: docker-compose restart backend"
echo "   3. Update PayOS dashboard webhook URL"
echo "   4. Test payment!"
echo ""
