#!/bin/bash

# Server Setup Script for Digital Ocean Droplet
# Run this script on your Digital Ocean droplet to prepare it for deployment

set -e

echo "🔧 Setting up Digital Ocean droplet for deployment..."

# Update system
echo "📦 Updating system packages..."
apt-get update && apt-get upgrade -y

# Install required packages
echo "📥 Installing required packages..."
apt-get install -y \
    curl \
    wget \
    git \
    ufw \
    htop \
    nginx \
    certbot \
    python3-certbot-nginx

# Install Docker
echo "🐳 Installing Docker..."
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
usermod -aG docker $USER

# Install Docker Compose
echo "🐳 Installing Docker Compose..."
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Install DigitalOcean CLI (doctl)
echo "🌊 Installing DigitalOcean CLI..."
cd /tmp
wget https://github.com/digitalocean/doctl/releases/download/v1.94.0/doctl-1.94.0-linux-amd64.tar.gz
tar xf doctl-1.94.0-linux-amd64.tar.gz
mv doctl /usr/local/bin

# Setup firewall
echo "🔥 Configuring firewall..."
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

# Create project directory
echo "📁 Creating project directories..."
mkdir -p /opt/movie-booking/{ssl,backups,logs}
chown -R $USER:$USER /opt/movie-booking

# Create systemd service for auto-start
echo "⚙️ Creating systemd service..."
cat > /etc/systemd/system/movie-booking.service << EOF
[Unit]
Description=Movie Booking API
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/movie-booking
ExecStart=/usr/local/bin/docker-compose -f docker-compose.prod.yml up -d
ExecStop=/usr/local/bin/docker-compose -f docker-compose.prod.yml down
User=$USER

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable movie-booking.service

# Setup log rotation
echo "📝 Setting up log rotation..."
cat > /etc/logrotate.d/movie-booking << EOF
/opt/movie-booking/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    copytruncate
}
EOF

# Setup backup cron job
echo "⏰ Setting up backup cron job..."
cat > /etc/cron.d/movie-booking-backup << EOF
# Backup database every day at 2 AM
0 2 * * * $USER cd /opt/movie-booking && ./deploy.sh backup
EOF

# Create swap file (recommended for 1GB droplets)
echo "💾 Creating swap file..."
if [[ ! -f /swapfile ]]; then
    fallocate -l 2G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' | tee -a /etc/fstab
fi

# Setup monitoring (basic)
echo "📊 Setting up basic monitoring..."
cat > /opt/movie-booking/monitor.sh << 'EOF'
#!/bin/bash
# Basic monitoring script

WEBHOOK_URL="$1"  # Optional Discord/Slack webhook for alerts

check_containers() {
    if ! docker ps | grep -q movie-booking-api-prod; then
        echo "❌ API container is not running!"
        return 1
    fi
    
    if ! docker ps | grep -q movie-booking-db-prod; then
        echo "❌ Database container is not running!"
        return 1
    fi
    
    return 0
}

check_health() {
    if ! curl -f -s http://localhost:8080/actuator/health > /dev/null; then
        echo "❌ Health check failed!"
        return 1
    fi
    
    return 0
}

check_disk_space() {
    USAGE=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')
    if [[ $USAGE -gt 80 ]]; then
        echo "⚠️ Disk usage is ${USAGE}%"
        return 1
    fi
    
    return 0
}

# Run checks
if ! check_containers || ! check_health || ! check_disk_space; then
    echo "$(date): System check failed" >> /opt/movie-booking/logs/monitor.log
    
    # Send alert if webhook is provided
    if [[ -n "$WEBHOOK_URL" ]]; then
        curl -X POST -H 'Content-type: application/json' \
            --data '{"text":"🚨 Movie Booking API Alert: System check failed on server"}' \
            "$WEBHOOK_URL"
    fi
    
    exit 1
else
    echo "$(date): All systems operational" >> /opt/movie-booking/logs/monitor.log
fi
EOF

chmod +x /opt/movie-booking/monitor.sh

# Add monitoring cron job
cat >> /etc/cron.d/movie-booking-backup << EOF

# Monitor system every 5 minutes
*/5 * * * * $USER /opt/movie-booking/monitor.sh
EOF

# Create welcome message
echo "📋 Creating server info..."
cat > /opt/movie-booking/server-info.txt << EOF
🎬 Movie Booking API Server Setup Complete!

Server Information:
- Project Directory: /opt/movie-booking
- Docker Compose File: docker-compose.prod.yml
- Environment File: .env.prod
- SSL Certificates: /opt/movie-booking/ssl/
- Logs: /opt/movie-booking/logs/
- Backups: /opt/movie-booking/backups/

Useful Commands:
- View logs: docker-compose -f /opt/movie-booking/docker-compose.prod.yml logs -f
- Restart services: systemctl restart movie-booking
- Check status: docker ps
- Deploy: cd /opt/movie-booking && ./deploy.sh

Next Steps:
1. Copy your .env.prod file to /opt/movie-booking/
2. Copy SSL certificates to /opt/movie-booking/ssl/ (optional)
3. Set up your GitHub secrets
4. Push to main branch to trigger deployment

EOF

echo "✅ Server setup complete!"
echo "📋 Check /opt/movie-booking/server-info.txt for important information"
echo ""
echo "🔑 Next steps:"
echo "1. Set up DigitalOcean Container Registry"
echo "2. Configure GitHub secrets"
echo "3. Copy environment file and SSL certificates"
echo "4. Test deployment"
echo ""
echo "⚠️ Don't forget to:"
echo "- Change default passwords"
echo "- Set up SSL certificates"
echo "- Configure domain DNS"
echo "- Test backup and monitoring"
