#!/bin/bash

# Script to add api.gocinema.io.vn to SSL certificate
# Run this on your VPS

set -e

echo "🔐 Adding api.gocinema.io.vn to SSL Certificate"
echo "=============================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

DOMAIN="gocinema.io.vn"
API_DOMAIN="api.gocinema.io.vn"
WWW_DOMAIN="www.gocinema.io.vn"

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}Please run as root (or use sudo)${NC}"
    exit 1
fi

echo -e "${BLUE}Step 1: Checking current SSL certificate...${NC}"
echo ""

# Check current certificate
if [ -f "/etc/letsencrypt/live/${DOMAIN}/fullchain.pem" ]; then
    echo "Current certificate domains:"
    openssl x509 -in /etc/letsencrypt/live/${DOMAIN}/fullchain.pem -text -noout | grep "DNS:" || echo "No DNS entries found"
    echo ""
else
    echo -e "${RED}No existing certificate found!${NC}"
    echo "Will create a new certificate..."
    echo ""
fi

echo -e "${BLUE}Step 2: Checking DNS records...${NC}"
echo ""

# Check if API subdomain DNS is configured
echo "Checking if ${API_DOMAIN} resolves to this server..."
API_IP=$(dig +short ${API_DOMAIN} | tail -n1)
SERVER_IP=$(curl -s ifconfig.me)

echo "API Domain resolves to: ${API_IP}"
echo "This server IP: ${SERVER_IP}"
echo ""

if [ "${API_IP}" != "${SERVER_IP}" ]; then
    echo -e "${YELLOW}⚠️  WARNING: DNS not pointing to this server!${NC}"
    echo ""
    echo "Please add DNS A record first:"
    echo "  Type: A"
    echo "  Name: api"
    echo "  Value: ${SERVER_IP}"
    echo "  TTL: 300 (or auto)"
    echo ""
    read -p "Have you added the DNS record? (yes/no): " dns_ready
    if [ "$dns_ready" != "yes" ]; then
        echo "Please add DNS record and try again."
        exit 1
    fi
fi

echo -e "${BLUE}Step 3: Checking Nginx configuration...${NC}"
echo ""

# Check if Nginx is installed
if ! command -v nginx &> /dev/null; then
    echo -e "${RED}Nginx is not installed!${NC}"
    echo "Installing Nginx..."
    apt update
    apt install -y nginx
fi

# Backup existing Nginx config
NGINX_CONF="/etc/nginx/sites-available/${DOMAIN}"
if [ -f "${NGINX_CONF}" ]; then
    echo "Backing up Nginx config..."
    cp ${NGINX_CONF} ${NGINX_CONF}.backup.$(date +%Y%m%d_%H%M%S)
fi

echo -e "${BLUE}Step 4: Creating/Updating Nginx configuration...${NC}"
echo ""

# Create Nginx config for API subdomain
cat > /etc/nginx/sites-available/${API_DOMAIN} << 'NGINX_CONFIG'
# API Backend - api.gocinema.io.vn
server {
    listen 80;
    server_name api.gocinema.io.vn;

    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.gocinema.io.vn;

    # SSL certificates (will be configured by certbot)
    ssl_certificate /etc/letsencrypt/live/gocinema.io.vn/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/gocinema.io.vn/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # API Backend proxy
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        
        # Headers
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

    # Health check
    location /actuator/health {
        proxy_pass http://localhost:8080/actuator/health;
        access_log off;
    }

    # Root location
    location / {
        return 200 'API Server Running';
        add_header Content-Type text/plain;
    }
}
NGINX_CONFIG

echo -e "${GREEN}✓ Nginx config created${NC}"

# Enable the site
if [ ! -L "/etc/nginx/sites-enabled/${API_DOMAIN}" ]; then
    ln -s /etc/nginx/sites-available/${API_DOMAIN} /etc/nginx/sites-enabled/
    echo -e "${GREEN}✓ Site enabled${NC}"
fi

# Test Nginx config
echo ""
echo "Testing Nginx configuration..."
nginx -t

if [ $? -ne 0 ]; then
    echo -e "${RED}Nginx configuration test failed!${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Nginx configuration is valid${NC}"
echo ""

echo -e "${BLUE}Step 5: Installing/Updating SSL certificate...${NC}"
echo ""

# Install certbot if not installed
if ! command -v certbot &> /dev/null; then
    echo "Installing certbot..."
    apt update
    apt install -y certbot python3-certbot-nginx
    echo -e "${GREEN}✓ Certbot installed${NC}"
fi

# Expand or create certificate
echo "Requesting SSL certificate for all domains..."
echo "Domains: ${DOMAIN}, ${WWW_DOMAIN}, ${API_DOMAIN}"
echo ""

certbot --nginx \
    -d ${DOMAIN} \
    -d ${WWW_DOMAIN} \
    -d ${API_DOMAIN} \
    --expand \
    --non-interactive \
    --agree-tos \
    --email contact@${DOMAIN} \
    --redirect

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ SSL certificate updated successfully!${NC}"
else
    echo ""
    echo -e "${RED}✗ Failed to update SSL certificate${NC}"
    echo "Please check the errors above"
    exit 1
fi

echo ""
echo -e "${BLUE}Step 6: Reloading Nginx...${NC}"
systemctl reload nginx

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Nginx reloaded${NC}"
else
    echo -e "${RED}✗ Failed to reload Nginx${NC}"
    exit 1
fi

echo ""
echo "=============================================="
echo -e "${GREEN}🎉 SUCCESS! SSL Certificate Updated${NC}"
echo "=============================================="
echo ""
echo "Certificate now includes:"
openssl x509 -in /etc/letsencrypt/live/${DOMAIN}/fullchain.pem -text -noout | grep "DNS:"
echo ""
echo "Next steps:"
echo "1. Test the endpoint:"
echo "   curl -I https://api.gocinema.io.vn/api/payos-webhook"
echo ""
echo "2. Update your backend .env:"
echo "   PAYOS_WEBHOOK_URL=https://api.gocinema.io.vn/api/payos-webhook"
echo ""
echo "3. Restart your backend service"
echo ""
echo "4. Update PayOS dashboard webhook URL:"
echo "   https://api.gocinema.io.vn/api/payos-webhook"
echo ""
echo "5. Test payment flow!"
echo ""
