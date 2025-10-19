#!/bin/bash

set -e

echo "🚀 Starting deployment..."

# Configuration
IMAGE_NAME="vuphongle23/movie-ticket-booking-be:latest"
CONTAINER_NAME="movie-booking-backend"
COMPOSE_FILE="docker-compose.yml"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${RED}❌ .env file not found!${NC}"
    echo "Please create .env file with required environment variables"
    exit 1
fi

echo -e "${YELLOW}📥 Pulling latest image from Docker Hub...${NC}"
docker pull $IMAGE_NAME

echo -e "${YELLOW}🔄 Stopping existing containers...${NC}"
docker-compose down || true

echo -e "${YELLOW}🧹 Cleaning up old images...${NC}"
docker image prune -f

echo -e "${YELLOW}🚀 Starting services with docker-compose...${NC}"
docker-compose up -d

echo -e "${YELLOW}⏳ Waiting for services to be healthy...${NC}"
sleep 10

# Check if backend is running
if docker ps | grep -q $CONTAINER_NAME; then
    echo -e "${GREEN}✅ Backend container is running${NC}"
    docker ps | grep $CONTAINER_NAME
else
    echo -e "${RED}❌ Backend container failed to start${NC}"
    docker logs $CONTAINER_NAME
    exit 1
fi

# Show logs
echo -e "${YELLOW}📋 Recent logs:${NC}"
docker-compose logs --tail=50 backend

echo -e "${GREEN}✅ Deployment completed successfully!${NC}"
echo -e "${GREEN}🌐 Application should be available at http://your-server:8080${NC}"
