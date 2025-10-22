#!/bin/bash

# SSL Setup Script for GoCinema VPS
# Usage: ./setup-ssl.sh [email]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() { echo -e "${BLUE}ℹ ${1}${NC}"; }
print_success() { echo -e "${GREEN}✓ ${1}${NC}"; }
print_warning() { echo -e "${YELLOW}⚠ ${1}${NC}"; }
print_error() { echo -e "${RED}✗ ${1}${NC}"; }

# Configuration
VPS_IP="159.223.38.127"
DOMAIN_USER="gocinema.io.vn"
DOMAIN_ADMIN="admin.gocinema.io.vn"
DOMAIN_API="api.gocinema.io.vn"
EMAIL="${1:-admin@gocinema.io.vn}"

echo ""
print_info "=== GoCinema SSL Setup Script ==="
echo ""
print_info "VPS IP: $VPS_IP"
print_info "Domains to secure:"
print_info "  - $DOMAIN_USER"
print_info "  - $DOMAIN_ADMIN"
print_info "  - $DOMAIN_API"
print_info "Email: $EMAIL"
echo ""

# Check if running on VPS or local
if [ "$(hostname -I | grep -c $VPS_IP)" -eq 0 ]; then
    print_error "This script must be run ON the VPS server"
    print_info "Please SSH to the server first:"
    print_info "  ssh root@$VPS_IP"
    print_info "Then run this script again"
    exit 1
fi

print_success "Running on VPS"
echo ""

# Step 1: Check DNS
print_info "Step 1: Checking DNS configuration..."
for domain in $DOMAIN_USER $DOMAIN_ADMIN $DOMAIN_API; do
    DNS_IP=$(dig +short $domain | tail -n1)
    if [ "$DNS_IP" == "$VPS_IP" ]; then
        print_success "$domain → $DNS_IP ✓"
    else
        print_error "$domain → $DNS_IP (expected $VPS_IP)"
        print_warning "Please configure DNS A record for $domain to point to $VPS_IP"
        read -p "Continue anyway? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
done
echo ""

# Step 2: Check Docker
print_info "Step 2: Checking Docker..."
if ! command -v docker &> /dev/null; then
    print_error "Docker not found"
    exit 1
fi
print_success "Docker installed"

# Check if containers are running
if docker ps | grep -q movie-booking-backend; then
    print_success "Backend container running"
else
    print_error "Backend container not running"
    exit 1
fi
echo ""

# Step 3: Check and open firewall
print_info "Step 3: Checking firewall..."
if command -v ufw &> /dev/null; then
    if ! ufw status | grep -q "443.*ALLOW"; then
        print_warning "Port 443 not open, opening now..."
        ufw allow 443/tcp
    fi
    print_success "Port 443 is open"
else
    print_warning "UFW not installed, skipping firewall check"
fi
echo ""

# Step 4: Stop current nginx
print_info "Step 4: Stopping current nginx container..."
if docker ps | grep -q gocinema-nginx-proxy; then
    docker stop gocinema-nginx-proxy
    print_success "Stopped gocinema-nginx-proxy"
else
    print_warning "gocinema-nginx-proxy not running"
fi
echo ""

# Step 5: Install certbot
print_info "Step 5: Installing certbot..."
if ! command -v certbot &> /dev/null; then
    print_info "Installing certbot..."
    apt update -qq
    apt install -y certbot > /dev/null 2>&1
    print_success "Certbot installed"
else
    print_success "Certbot already installed"
fi
echo ""

# Step 6: Obtain certificates
print_info "Step 6: Obtaining SSL certificates..."
print_warning "This may take a few minutes..."
echo ""

# Function to get certificate
get_cert() {
    local domain=$1
    local extra_domains=$2
    
    print_info "Getting certificate for $domain..."
    
    if [ -d "/etc/letsencrypt/live/$domain" ]; then
        print_warning "Certificate already exists for $domain"
        read -p "Renew? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            certbot certonly --standalone --force-renewal \
                --email $EMAIL \
                --agree-tos \
                --no-eff-email \
                -d $domain $extra_domains
        fi
    else
        certbot certonly --standalone \
            --email $EMAIL \
            --agree-tos \
            --no-eff-email \
            -d $domain $extra_domains
    fi
    
    if [ $? -eq 0 ]; then
        print_success "Certificate obtained for $domain"
    else
        print_error "Failed to obtain certificate for $domain"
        return 1
    fi
}

# Get certificates for each domain
get_cert "$DOMAIN_USER" "-d www.$DOMAIN_USER" || exit 1
get_cert "$DOMAIN_ADMIN" "" || exit 1
get_cert "$DOMAIN_API" "" || exit 1

echo ""
print_success "All certificates obtained successfully!"
echo ""

# Step 7: Create nginx SSL config
print_info "Step 7: Creating nginx SSL configuration..."

mkdir -p /etc/nginx-ssl/conf.d

# Create SSL nginx.conf
cat > /etc/nginx-ssl/nginx.conf << 'NGINXCONF'
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

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
               application/json application/javascript application/xml+rss;

    # HTTP to HTTPS redirect - User Frontend
    server {
        listen 80;
        server_name gocinema.io.vn www.gocinema.io.vn;
        return 301 https://gocinema.io.vn$request_uri;
    }

    # HTTPS User Frontend
    server {
        listen 443 ssl http2;
        server_name gocinema.io.vn www.gocinema.io.vn;

        ssl_certificate /etc/letsencrypt/live/gocinema.io.vn/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/gocinema.io.vn/privkey.pem;

        access_log /var/log/nginx/gocinema-user-access.log main;
        error_log /var/log/nginx/gocinema-user-error.log warn;

        add_header Strict-Transport-Security "max-age=31536000" always;
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
            proxy_set_header X-Forwarded-Proto https;
            proxy_cache_bypass $http_upgrade;
        }
    }

    # HTTP to HTTPS redirect - Admin
    server {
        listen 80;
        server_name admin.gocinema.io.vn;
        return 301 https://admin.gocinema.io.vn$request_uri;
    }

    # HTTPS Admin Frontend
    server {
        listen 443 ssl http2;
        server_name admin.gocinema.io.vn;

        ssl_certificate /etc/letsencrypt/live/admin.gocinema.io.vn/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/admin.gocinema.io.vn/privkey.pem;

        access_log /var/log/nginx/gocinema-admin-access.log main;
        error_log /var/log/nginx/gocinema-admin-error.log warn;

        add_header Strict-Transport-Security "max-age=31536000" always;
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
            proxy_set_header X-Forwarded-Proto https;
            proxy_cache_bypass $http_upgrade;
        }
    }

    # HTTP to HTTPS redirect - API
    server {
        listen 80;
        server_name api.gocinema.io.vn;
        return 301 https://api.gocinema.io.vn$request_uri;
    }

    # HTTPS Backend API
    server {
        listen 443 ssl http2;
        server_name api.gocinema.io.vn;

        ssl_certificate /etc/letsencrypt/live/api.gocinema.io.vn/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/api.gocinema.io.vn/privkey.pem;

        access_log /var/log/nginx/gocinema-api-access.log main;
        error_log /var/log/nginx/gocinema-api-error.log warn;

        add_header Strict-Transport-Security "max-age=31536000" always;

        location /api/ {
            proxy_pass http://movie-booking-backend:8080/api/;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;
            proxy_set_header x-payos-signature $http_x_payos_signature;
            
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
        
        return 444;
    }
}
NGINXCONF

print_success "Nginx SSL config created"
echo ""

# Step 8: Start SSL nginx
print_info "Step 8: Starting SSL-enabled nginx..."

# Remove old container if exists
docker rm -f gocinema-nginx-proxy 2>/dev/null || true

# Get network name
NETWORK=$(docker inspect movie-booking-backend | grep -A 5 '"Networks"' | grep -o '"[^"]*-network"' | tr -d '"' | head -1)
if [ -z "$NETWORK" ]; then
    NETWORK="gocinema-network"
fi

# Start new nginx with SSL
docker run -d \
    --name gocinema-nginx-ssl \
    --restart unless-stopped \
    --network $NETWORK \
    -p 80:80 \
    -p 443:443 \
    -v /etc/nginx-ssl/nginx.conf:/etc/nginx/nginx.conf:ro \
    -v /etc/letsencrypt:/etc/letsencrypt:ro \
    nginx:alpine

if [ $? -eq 0 ]; then
    print_success "Nginx SSL container started"
else
    print_error "Failed to start nginx"
    exit 1
fi
echo ""

# Step 9: Setup auto-renewal
print_info "Step 9: Setting up auto-renewal..."

# Create renewal script
cat > /root/renew-certs.sh << 'RENEWSCRIPT'
#!/bin/bash
certbot renew --quiet
docker exec gocinema-nginx-ssl nginx -s reload
RENEWSCRIPT

chmod +x /root/renew-certs.sh

# Add to crontab if not exists
(crontab -l 2>/dev/null | grep -v renew-certs; echo "0 0,12 * * * /root/renew-certs.sh >> /var/log/cert-renewal.log 2>&1") | crontab -

print_success "Auto-renewal configured (runs twice daily)"
echo ""

# Step 10: Test SSL
print_info "Step 10: Testing SSL configuration..."
sleep 5

test_url() {
    local url=$1
    local status=$(curl -I -s -o /dev/null -w "%{http_code}" $url --connect-timeout 10)
    if [ "$status" == "200" ] || [ "$status" == "301" ] || [ "$status" == "302" ]; then
        print_success "$url → HTTP $status ✓"
        return 0
    else
        print_error "$url → HTTP $status ✗"
        return 1
    fi
}

test_url "https://$DOMAIN_USER"
test_url "https://$DOMAIN_ADMIN"
test_url "https://$DOMAIN_API/health"

echo ""
print_info "=== SSL Setup Complete! ==="
echo ""
print_success "Your domains are now secured with SSL:"
print_info "  ✓ https://$DOMAIN_USER"
print_info "  ✓ https://$DOMAIN_ADMIN"
print_info "  ✓ https://$DOMAIN_API"
echo ""
print_info "Next steps:"
print_info "  1. Test your websites in browser"
print_info "  2. Configure PayOS webhook URL:"
print_info "     https://api.gocinema.io.vn/api/payos-webhook"
print_info "  3. Update backend env: PAYOS_WEBHOOK_URL"
print_info "  4. Test SSL grade: https://www.ssllabs.com/ssltest/"
echo ""
print_info "Certificates will auto-renew every 12 hours"
print_info "Check renewal logs: /var/log/cert-renewal.log"
echo ""
print_success "Done! 🎉"
