git p# 🎬 Movie Ticket Booking - Full Deployment Guide

## 📊 System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    VPS (128.199.113.207)                │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────┐         ┌──────────────────┐     │
│  │  Frontend Admin │◄────────┤  Nginx (Port 80) │     │
│  │   (React+Vite)  │         └──────────────────┘     │
│  └────────┬────────┘                                   │
│           │ /api/*                                     │
│           ▼                                            │
│  ┌─────────────────┐         ┌──────────────────┐     │
│  │   Backend API   │◄────────┤  Spring Boot     │     │
│  │   (Port 8080)   │         │  (Port 8080)     │     │
│  └────────┬────────┘         └──────────────────┘     │
│           │                                            │
│           ▼                                            │
│  ┌─────────────────┐                                  │
│  │    MariaDB      │                                  │
│  │   (Port 3306)   │                                  │
│  └─────────────────┘                                  │
│                                                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                    CI/CD Pipeline                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  GitHub Push → Jenkins → Build → Docker Hub → VPS      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## 🚀 Quick Deployment Steps

### 1️⃣ Backend (DONE ✅)

```bash
# Trên VPS
cd /root/movie-ticket-booking-be
docker compose up -d

# Verify
docker ps
curl http://localhost:8080/actuator/health
```

**Services:**

- Backend API: `http://128.199.113.207:8080`
- MariaDB: `localhost:3306` (internal)

---

### 2️⃣ Frontend Admin (TODO)

#### Step 1: Setup Jenkins Pipeline cho FE

1. Vào Jenkins Dashboard
2. Tạo **New Item** → **Pipeline**
3. Tên: `movie-booking-frontend-admin`
4. Pipeline from SCM:
   - Repository: `https://github.com/vuphongle/movie-ticket-booking-fe-web-admin.git`
   - Branch: `feature/dashboard--revenue-cinema-and-movie` (hoặc `main`)
   - Script Path: `Jenkinsfile`
5. Save và **Build Now**

#### Step 2: Deploy Frontend lên VPS

**Sau khi Jenkins build xong:**

```bash
# SSH vào VPS
ssh root@128.199.113.207

# Option A: Quick Deploy (Recommended)
docker pull vuphongle23/movie-ticket-booking-fe-admin:latest

docker run -d \
  --name movie-booking-frontend-admin \
  --network movie-ticket-booking-be_movie-booking-network \
  -p 80:80 \
  --restart unless-stopped \
  vuphongle23/movie-ticket-booking-fe-admin:latest

# Verify
docker ps | grep frontend
curl http://localhost

# Option B: Dùng deployment script
cd /root
git clone https://github.com/vuphongle/movie-ticket-booking-fe-web-admin.git
cd movie-ticket-booking-fe-web-admin
git checkout feature/dashboard--revenue-cinema-and-movie
chmod +x deploy-fe.sh
./deploy-fe.sh
```

---

## 📋 Complete Checklist

### Backend ✅

- [x] Dockerfile created
- [x] Jenkinsfile created
- [x] Docker image built and pushed to Docker Hub
- [x] Jenkins pipeline configured
- [x] .env file configured on VPS
- [x] MariaDB container running
- [x] Backend container running
- [x] API responding correctly

### Frontend 🔄

- [x] Dockerfile created
- [x] nginx.conf configured
- [x] Jenkinsfile created
- [x] deploy-fe.sh script created
- [ ] Jenkins pipeline configured for FE
- [ ] Docker image built and pushed to Docker Hub
- [ ] Frontend deployed to VPS
- [ ] Frontend accessible via browser

---

## 🔗 Access URLs

### Current (Backend Only)

- **Backend API**: http://128.199.113.207:8080
- **Health Check**: http://128.199.113.207:8080/actuator/health
- **API Docs**: http://128.199.113.207:8080/swagger-ui.html (if enabled)

### After Frontend Deploy

- **Admin Frontend**: http://128.199.113.207 (port 80)
- **API via Nginx**: http://128.199.113.207/api/*

---

## 🔧 Common Commands

### Backend

```bash
# Logs
docker logs movie-booking-backend -f

# Restart
docker restart movie-booking-backend

# Update
cd /root/movie-ticket-booking-be
git pull origin dev
./deploy.sh
```

### Frontend

```bash
# Logs
docker logs movie-booking-frontend-admin -f

# Restart
docker restart movie-booking-frontend-admin

# Update
cd /root/movie-ticket-booking-fe-web-admin
./deploy-fe.sh
```

### Database

```bash
# Logs
docker logs movie-booking-mariadb

# Access MySQL CLI
docker exec -it movie-booking-mariadb mysql -u movie_user -p

# Backup
docker exec movie-booking-mariadb mysqldump -u root -p db_movie_ticket_booking > backup.sql
```

---

## 🐛 Troubleshooting

### Backend không kết nối DB

```bash
docker logs movie-booking-backend | grep -i error
docker exec movie-booking-backend ping mariadb
```

### Frontend không gọi được API

```bash
# Kiểm tra network
docker network inspect movie-ticket-booking-be_movie-booking-network

# Test từ frontend container
docker exec movie-booking-frontend-admin wget -O- http://movie-booking-backend:8080/actuator/health
```

### Port conflict

```bash
# Tìm process đang dùng port
lsof -i :80
lsof -i :8080

# Stop conflicting services
systemctl stop apache2
systemctl stop nginx  # nếu nginx chạy native
```

---

## 🔐 Security Checklist

- [ ] Change default database passwords
- [ ] Use strong JWT secret (32+ characters)
- [ ] Setup firewall (ufw)
- [ ] Setup HTTPS/SSL (Let's Encrypt)
- [ ] Configure rate limiting
- [ ] Regular security updates
- [ ] Backup strategy

---

## 📈 Next Steps

1. **Deploy Frontend** (main task now)
2. Setup custom domain
3. Configure HTTPS/SSL
4. Setup monitoring (Prometheus + Grafana)
5. Configure auto-scaling
6. Implement backup automation
7. Setup logging aggregation
8. Configure CDN (CloudFlare)

---

## 🆘 Support & Resources

### Documentation

- Backend: `/root/movie-ticket-booking-be/DEPLOYMENT.md`
- Frontend: `/root/movie-ticket-booking-fe-web-admin/DEPLOYMENT.md`

### Docker Images

- Backend: `vuphongle23/movie-ticket-booking-be:latest`
- Frontend: `vuphongle23/movie-ticket-booking-fe-admin:latest`

### Repositories

- Backend: https://github.com/vuphongle/movie-ticket-booking-be
- Frontend: https://github.com/vuphongle/movie-ticket-booking-fe-web-admin

---

## 📞 Contact

- VPS IP: 128.199.113.207
- SSH: `ssh root@128.199.113.207`
- Jenkins: Configure in your server
