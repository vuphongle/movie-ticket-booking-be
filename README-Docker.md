# Docker Setup cho Movie Ticket Booking Backend

## 🚀 Quick Start

```bash
# 1. Setup environment
cp .env.example .env
# Edit .env với database credentials

# 2. Build & Run
docker build -t movie-ticket-booking-be .
docker run -d --name movie-booking-api -p 8080:8080 --env-file .env movie-ticket-booking-be

# 3. Hoặc sử dụng docker-compose (recommended)
docker-compose up -d

# 4. Hoặc dùng helper script
./docker.sh build && ./docker.sh run

# 5. Check status
curl http://localhost:8080/actuator/health
```

**Image tối ưu**: 466MB (Distroless Java17 + Multi-stage build)

### Helper Script Commands

```bash
./docker.sh build    # Build image
./docker.sh run      # Run container
./docker.sh stop     # Stop container
./docker.sh logs     # View logs
./docker.sh status   # Check status
./docker.sh clean    # Cleanup

## Tổng quan

Dự án này sử dụng Docker để containerize ứng dụng Spring Boot, giúp dễ dàng triển khai trên các môi trường khác nhau như EC2, Digital Ocean, hoặc bất kỳ platform nào hỗ trợ Docker.

## Cấu trúc Files

```

├── Dockerfile # Optimized production image (466MB)
├── docker-compose.yml # Development với MariaDB
├── docker.sh # Helper script (build, run, stop)
├── .dockerignore # Build optimization
├── .env.example # Environment template
└── README-Docker.md # Hướng dẫn này

````

## Prerequisites

- Docker >= 20.x
- Docker Compose >= 2.x (nếu sử dụng)
- Java 17 (cho development)
- MariaDB/MySQL database

## Environment Variables

### Required Variables

Các biến môi trường bắt buộc cần được set:

```bash
# Database Configuration
DB_HOST=your_database_host
DB_PORT=3306
DB_NAME=db-movie-ticket-booking
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# JWT Security
JWT_SECRET=your_jwt_secret_key_minimum_256_bits

# Email Configuration
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_gmail_app_password

# Frontend Configuration
FRONTEND_HOST=http://your-frontend-domain
FRONTEND_PORT=3000
````

### Optional Variables

```bash
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080
```

## Docker Image Info

Image được tối ưu với multi-stage build:

- **Build Stage**: Eclipse Temurin 17 JDK (Ubuntu-based)
- **Runtime Stage**: Distroless Java 17 (Google)
- **Size**: ~466MB (giảm 68% so với JDK full)
- **Architecture**: Multi-platform (ARM64 & AMD64 compatible)
- **Security**:
  - Distroless base (minimal attack surface)
  - Non-root user (nonroot)
  - No shell/package manager trong runtime
- **Performance**:
  - JVM memory tuning (256MB-512MB heap)
  - G1 garbage collector
  - String deduplication enabled

## Build và Run Commands

### 1. Build Docker Image

```bash
# Build image với tag
docker build -t movie-ticket-booking-be:latest .

# Build production variant (minimal)
docker build -f Dockerfile.prod -t movie-ticket-booking-be:prod .

# Note: Build time khoảng 2-3 phút, tối ưu cho cache layers
# Final image: 466MB (giảm 68% từ 1.45GB)
```

### 2. Run Container

```bash
# Run với environment variables
docker run -d \
  --name movie-booking-api \
  -p 8080:8080 \
  -e DB_HOST=your_db_host \
  -e DB_USERNAME=your_username \
  -e DB_PASSWORD=your_password \
  -e JWT_SECRET=your_jwt_secret \
  -e MAIL_USERNAME=your_email@gmail.com \
  -e MAIL_PASSWORD=your_app_password \
  movie-ticket-booking-be:latest
```

### 3. Run với Environment File

Tạo file `.env`:

```bash
# .env file
DB_HOST=localhost
DB_PORT=3306
DB_NAME=db-movie-ticket-booking
DB_USERNAME=movie_user
DB_PASSWORD=secure_password
JWT_SECRET=your_very_long_jwt_secret_key_here
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_gmail_app_password
FRONTEND_HOST=http://localhost
FRONTEND_PORT=3000
SPRING_PROFILES_ACTIVE=production
```

Sau đó run:

```bash
docker run -d \
  --name movie-booking-api \
  -p 8080:8080 \
  --env-file .env \
  movie-ticket-booking-be:latest
```

## Docker Compose Setup (Recommended)

Tạo file `docker-compose.yml` cho complete setup:

```yaml
version: "3.8"

services:
  # Database Service
  mariadb:
    image: mariadb:10.11
    container_name: movie-booking-db
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: db-movie-ticket-booking
      MYSQL_USER: movie_user
      MYSQL_PASSWORD: secure_password
    ports:
      - "3306:3306"
    volumes:
      - mariadb_data:/var/lib/mysql
    networks:
      - movie-booking-network
    restart: unless-stopped

  # Application Service
  app:
    build: .
    container_name: movie-booking-api
    environment:
      SPRING_PROFILES_ACTIVE: production
      DB_HOST: mariadb
      DB_PORT: 3306
      DB_NAME: db-movie-ticket-booking
      DB_USERNAME: movie_user
      DB_PASSWORD: secure_password
      JWT_SECRET: your_jwt_secret_key_here
      MAIL_USERNAME: your_email@gmail.com
      MAIL_PASSWORD: your_gmail_app_password
      FRONTEND_HOST: http://localhost
      FRONTEND_PORT: 3000
    ports:
      - "8080:8080"
    depends_on:
      - mariadb
    networks:
      - movie-booking-network
    restart: unless-stopped

volumes:
  mariadb_data:

networks:
  movie-booking-network:
    driver: bridge
```

Run với Docker Compose:

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down

# Remove volumes (careful!)
docker-compose down -v
```

## Deployment Commands

### EC2 Deployment

```bash
# 1. Build và push image lên Docker Hub
docker build -t yourusername/movie-ticket-booking-be:latest .
docker push yourusername/movie-ticket-booking-be:latest

# 2. Trên EC2 instance
docker pull yourusername/movie-ticket-booking-be:latest
docker run -d \
  --name movie-booking-api \
  -p 8080:8080 \
  --env-file .env \
  --restart unless-stopped \
  yourusername/movie-ticket-booking-be:latest
```

### Digital Ocean Deployment

```bash
# Tương tự EC2, hoặc sử dụng Digital Ocean App Platform
# Upload source code và Dockerfile lên GitHub
# Connect repository với Digital Ocean App Platform
```

## Health Check

Container có built-in health check:

```bash
# Check container health
docker ps

# Manual health check
curl http://localhost:8080/actuator/health
```

## Troubleshooting

### 1. Container không start

```bash
# Check logs
docker logs movie-booking-api

# Check environment variables
docker exec movie-booking-api env
```

### 2. Database connection issues

```bash
# Check database connectivity
docker exec movie-booking-api nslookup mariadb

# Test database connection
docker exec -it mariadb mysql -u movie_user -p
```

### 3. Memory issues

```bash
# Run với memory limit
docker run -d \
  --name movie-booking-api \
  -p 8080:8080 \
  --memory="1g" \
  --memory-swap="2g" \
  movie-ticket-booking-be:latest
```

## Best Practices

### 1. Security

- Không hardcode passwords trong Dockerfile
- Sử dụng secrets management trong production
- Run container với non-root user
- Regularly update base images

### 2. Performance

- Sử dụng multi-stage builds nếu cần
- Optimize Docker layers
- Set appropriate JVM heap size
- Use health checks

### 3. Monitoring

- Implement proper logging
- Use container monitoring tools
- Set up alerts for container health

## Next Steps cho CI/CD

### GitHub Actions Workflow

Chuẩn bị cho file `.github/workflows/deploy.yml`:

```yaml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build Docker image
        run: docker build -t movie-ticket-booking-be .

      - name: Deploy to server
        # Sẽ được implement sau
        run: echo "Deploy to EC2/Digital Ocean"
```

### Cần chuẩn bị:

1. Docker Hub account (hoặc container registry khác)
2. EC2 instance hoặc Digital Ocean droplet
3. Database setup trên production
4. Domain name và SSL certificate
5. Environment variables cho production

## Support

Nếu gặp vấn đề, check:

1. Docker logs: `docker logs container_name`
2. Application logs inside container
3. Database connectivity
4. Environment variables configuration
