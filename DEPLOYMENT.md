# 🚀 CI/CD Deployment Guide - Digital Ocean

Hướng dẫn setup CI/CD cho Movie Ticket Booking API với GitHub Actions và Digital Ocean.

## 📋 Tổng quan

Hệ thống CI/CD này sẽ:

- ✅ Chạy tests tự động khi có code mới
- 🐳 Build Docker image và push lên DigitalOcean Container Registry
- 🚀 Deploy tự động lên Digital Ocean Droplet
- 🔄 Rollback tự động nếu deployment failed
- 📊 Monitoring và alerting cơ bản

## 🛠️ Bước 1: Setup Digital Ocean

### 1.1 Tạo Droplet

```bash
# Khuyến nghị cấu hình tối thiểu:
# - 2GB RAM, 1 vCPU, 50GB SSD
# - Ubuntu 22.04 LTS
# - Khu vực: Singapore (gần Việt Nam)
```

### 1.2 Tạo Container Registry

1. Vào DigitalOcean Console
2. Tạo Container Registry (tên: `movie-booking-registry`)
3. Lưu lại registry name

### 1.3 Tạo API Token

1. Vào Settings > API > Personal Access Tokens
2. Tạo token mới với quyền `read` và `write`
3. Lưu lại token này

## 🔧 Bước 2: Setup Server

### 2.1 Kết nối SSH và chạy setup script

```bash
# SSH vào droplet
ssh root@your-droplet-ip

# Tải và chạy setup script
wget https://raw.githubusercontent.com/your-username/movie-ticket-booking-be/main/setup-server.sh
chmod +x setup-server.sh
./setup-server.sh
```

### 2.2 Tạo SSH Key cho GitHub Actions

```bash
# Tạo SSH key mới
ssh-keygen -t rsa -b 4096 -C "github-actions" -f ~/.ssh/github-actions
cat ~/.ssh/github-actions.pub >> ~/.ssh/authorized_keys

# Copy private key để add vào GitHub secrets
cat ~/.ssh/github-actions
```

### 2.3 Copy files cần thiết

```bash
# Copy docker-compose và script
cd /opt/movie-booking
git clone https://github.com/your-username/movie-ticket-booking-be.git temp
cp temp/docker-compose.prod.yml .
cp temp/deploy.sh .
cp temp/nginx.prod.conf .
cp temp/.env.prod.template .env.prod
rm -rf temp

# Chỉnh sửa file .env.prod với thông tin thực tế
nano .env.prod
```

## ⚙️ Bước 3: Setup GitHub Secrets

Vào GitHub Repository Settings > Secrets and variables > Actions, thêm các secrets:

### 3.1 Digital Ocean Secrets

```
DIGITALOCEAN_ACCESS_TOKEN=dop_v1_xxxxxxxxxxxxx
DO_REGISTRY_NAME=movie-booking-registry
DO_HOST=your-droplet-ip
DO_USERNAME=root
DO_PORT=22
DO_SSH_KEY=-----BEGIN OPENSSH PRIVATE KEY-----
(copy nội dung từ ~/.ssh/github-actions)
-----END OPENSSH PRIVATE KEY-----
```

### 3.2 Database Secrets (optional - for backup)

```
DB_USERNAME=movie_user
DB_PASSWORD=your-secure-password
DB_NAME=db-movie-ticket-booking
```

## 🎯 Bước 4: Test Deployment

### 4.1 Push code lên main branch

```bash
git add .
git commit -m "feat: setup CI/CD with GitHub Actions"
git push origin main
```

### 4.2 Kiểm tra GitHub Actions

1. Vào repository > Actions tab
2. Xem workflow đang chạy
3. Kiểm tra từng bước trong pipeline

### 4.3 Verify deployment

```bash
# SSH vào server và kiểm tra
ssh root@your-droplet-ip

# Check containers
docker ps

# Check logs
cd /opt/movie-booking
docker-compose -f docker-compose.prod.yml logs -f

# Test API
curl http://localhost:8080/actuator/health
```

## 🔒 Bước 5: Setup SSL (Optional nhưng khuyến nghị)

### 5.1 Setup domain

```bash
# Point your domain to droplet IP
# A record: api.yourdomain.com -> your-droplet-ip
```

### 5.2 Get SSL certificate

```bash
# SSH vào server
certbot --nginx -d api.yourdomain.com

# Update nginx config
nano /opt/movie-booking/nginx.prod.conf
# Uncomment SSL lines và update domain name

# Restart nginx
docker-compose -f docker-compose.prod.yml restart nginx
```

## 📊 Bước 6: Monitoring Setup

### 6.1 Setup log monitoring

```bash
# View real-time logs
docker-compose -f /opt/movie-booking/docker-compose.prod.yml logs -f

# Setup log aggregation (optional)
# Install ELK stack or use DigitalOcean Monitoring
```

### 6.2 Setup alerts (optional)

```bash
# Add Discord/Slack webhook to monitoring script
nano /opt/movie-booking/monitor.sh

# Add webhook URL
/opt/movie-booking/monitor.sh "https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
```

## 🛠️ Troubleshooting

### Common Issues

#### 1. Docker Registry Login Failed

```bash
# Check token permissions
doctl auth init --access-token your-token
doctl registry login
```

#### 2. Container Won't Start

```bash
# Check logs
docker-compose -f docker-compose.prod.yml logs app

# Check environment variables
docker-compose -f docker-compose.prod.yml config
```

#### 3. Database Connection Failed

```bash
# Check database is running
docker ps | grep mariadb

# Check database logs
docker logs movie-booking-db-prod

# Test connection
docker exec -it movie-booking-db-prod mysql -u movie_user -p
```

#### 4. GitHub Actions Failed

```bash
# Check secrets are set correctly
# Verify SSH key has correct permissions
# Check DigitalOcean token is valid
```

## 📝 Useful Commands

### Server Management

```bash
# Deploy manually
cd /opt/movie-booking && ./deploy.sh

# Restart all services
systemctl restart movie-booking

# View system status
docker ps
docker stats

# Check disk space
df -h

# View logs
tail -f /opt/movie-booking/logs/*.log
```

### Database Management

```bash
# Backup database
docker exec movie-booking-db-prod mysqldump -u movie_user -p db-movie-ticket-booking > backup.sql

# Restore database
docker exec -i movie-booking-db-prod mysql -u movie_user -p db-movie-ticket-booking < backup.sql

# Access database
docker exec -it movie-booking-db-prod mysql -u movie_user -p
```

### Performance Tuning

```bash
# Monitor resource usage
htop
docker stats

# Optimize Java heap size in docker-compose.prod.yml
JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC"

# Scale containers (if needed)
docker-compose -f docker-compose.prod.yml up -d --scale app=2
```

## 🔄 Workflow Overview

```mermaid
graph TD
    A[Push to main] --> B[GitHub Actions]
    B --> C[Run Tests]
    C --> D[Build Docker Image]
    D --> E[Push to DO Registry]
    E --> F[Deploy to Droplet]
    F --> G[Health Check]
    G --> H{Success?}
    H -->|Yes| I[✅ Deployment Complete]
    H -->|No| J[🔄 Rollback]
    J --> K[❌ Notify Failure]
```

## 📞 Support

Nếu gặp vấn đề:

1. Kiểm tra logs trong GitHub Actions
2. SSH vào server và check container logs
3. Verify tất cả secrets đã setup đúng
4. Check DigitalOcean status page

---

🎉 **Chúc mừng! Bạn đã setup thành công CI/CD pipeline!** 🎉
