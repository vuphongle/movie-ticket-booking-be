# 🚀 Deployment Guide

## Prerequisites

- Docker and Docker Compose installed on VPS
- Access to VPS via SSH
- Docker Hub account (already configured)

## 📋 Steps to Deploy on VPS

### 1. SSH into your VPS

```bash
ssh your-user@your-vps-ip
```

### 2. Clone the repository (first time only)

```bash
git clone https://github.com/vuphongle/movie-ticket-booking-be.git
cd movie-ticket-booking-be
```

### 3. Switch to dev branch

```bash
git checkout dev
```

### 4. Create `.env` file

```bash
cp .env.example .env
nano .env  # or use vim/vi to edit
```

Fill in all required environment variables:
- Database credentials
- JWT secret (minimum 32 characters)
- Email configuration (Gmail App Password)
- AWS S3 credentials
- OpenAI API key
- PayOS credentials

### 5. Run the deployment script

```bash
chmod +x deploy.sh
./deploy.sh
```

The script will:
- Pull the latest Docker image from Docker Hub
- Stop existing containers
- Clean up old images
- Start all services (MariaDB + Backend)
- Show health status and logs

### 6. Verify deployment

```bash
# Check running containers
docker ps

# Check backend logs
docker logs movie-booking-backend -f

# Check if application is responding
curl http://localhost:8080/actuator/health
```

## 🔄 Update to Latest Version

When Jenkins builds a new version:

```bash
cd movie-ticket-booking-be
git pull origin dev
./deploy.sh
```

## 🔍 Troubleshooting

### View logs
```bash
# All services
docker-compose logs -f

# Backend only
docker-compose logs -f backend

# Database only
docker-compose logs -f mariadb
```

### Restart services
```bash
docker-compose restart backend
```

### Stop all services
```bash
docker-compose down
```

### Clean up everything (⚠️ Warning: This will delete database data!)
```bash
docker-compose down -v
```

## 📊 Monitoring

### Check service health
```bash
docker ps
docker-compose ps
```

### Check resource usage
```bash
docker stats
```

## 🌐 Access Points

- **Backend API**: `http://your-vps-ip:8080`
- **Health Check**: `http://your-vps-ip:8080/actuator/health`
- **Database**: `your-vps-ip:3307` (from outside VPS)

## 🔒 Security Notes

1. **Never commit `.env` file** - it contains sensitive credentials
2. Change default passwords in production
3. Use strong JWT secret (minimum 32 characters, random string)
4. Enable firewall and only expose necessary ports
5. Use HTTPS in production (setup nginx as reverse proxy)

## 📝 CI/CD Workflow

```
Developer push code → GitHub
           ↓
    Jenkins webhook triggered
           ↓
    Jenkins builds Docker image
           ↓
    Push to Docker Hub
           ↓
    SSH to VPS and run ./deploy.sh
           ↓
    Application updated! ✅
```

## 🆘 Support

If you encounter any issues, check:
1. Docker logs: `docker-compose logs`
2. Application logs: `./logs/spring.log`
3. Database connectivity
4. Environment variables in `.env` file
