#!/bin/bash

# Script to properly add API server block to nginx SSL config

set -e

SSH_HOST="root@159.223.38.127"
NGINX_CONF="/etc/nginx-ssl/nginx.conf"

echo "Creating new nginx config with API subdomain..."

# Download current config
scp ${SSH_HOST}:${NGINX_CONF} /tmp/nginx-ssl.conf.current

# Create new config with API block inserted before default server
cat > /tmp/nginx-ssl-api-block.txt << 'EOF'

    # API Backend - api.gocinema.io.vn
    server {
        listen 80;
        server_name api.gocinema.io.vn;
        return 301 https://$host$request_uri;
    }

    server {
        listen 443 ssl http2;
        server_name api.gocinema.io.vn;

        ssl_certificate /etc/letsencrypt/live/gocinema.io.vn/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/gocinema.io.vn/privkey.pem;

        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-XSS-Protection "1; mode=block" always;

        location /api/ {
            proxy_pass http://movie-booking-backend:8080/api/;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_set_header x-payos-signature $http_x_payos_signature;
            
            proxy_connect_timeout 60s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
        }

        location /health {
            return 200 'API healthy\n';
            add_header Content-Type text/plain;
        }
    }
EOF

# Insert API block before the last default server block
sed -i.bak '/server {/{
    :a
    N
    /listen 80 default_server;/!ba
    r /tmp/nginx-ssl-api-block.txt
}' /tmp/nginx-ssl.conf.current

echo "Config updated. Uploading..."

# Upload new config
scp /tmp/nginx-ssl.conf.current ${SSH_HOST}:${NGINX_CONF}.new

# Test and apply
ssh ${SSH_HOST} "
    docker exec gocinema-nginx-ssl nginx -t -c /etc/nginx/nginx.conf.new
    if [ \$? -eq 0 ]; then
        mv ${NGINX_CONF} ${NGINX_CONF}.old
        mv ${NGINX_CONF}.new ${NGINX_CONF}
        docker restart gocinema-nginx-ssl
        echo 'SUCCESS!'
    else
        echo 'Config test failed!'
        exit 1
    fi
"

echo "Done!"
