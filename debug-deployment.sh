#!/bin/bash

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Debugging Deployment Issues${NC}"
echo -e "${BLUE}========================================${NC}\n"

# 1. Check Docker Compose status
echo -e "${YELLOW}1. Docker Compose Services Status:${NC}"
docker-compose ps
echo ""

# 2. Check if containers are running
echo -e "${YELLOW}2. Running Containers:${NC}"
docker ps -a | grep movie-booking
echo ""

# 3. Check network
echo -e "${YELLOW}3. Docker Network:${NC}"
docker network ls | grep movie-booking
echo ""

# 4. Inspect network connections
echo -e "${YELLOW}4. Network Connections:${NC}"
docker network inspect movie-ticket-booking-be_movie-booking-network 2>/dev/null || \
docker network inspect movie-booking-network 2>/dev/null || \
echo "Network not found"
echo ""

# 5. Check MariaDB logs
echo -e "${YELLOW}5. MariaDB Logs (last 30 lines):${NC}"
docker logs movie-booking-mariadb --tail 30
echo ""

# 6. Check Backend logs
echo -e "${YELLOW}6. Backend Logs (last 50 lines):${NC}"
docker logs movie-booking-backend --tail 50
echo ""

# 7. Test database connection from backend container
echo -e "${YELLOW}7. Testing DB connection from backend container:${NC}"
docker exec movie-booking-backend ping -c 2 mariadb 2>/dev/null || \
echo -e "${RED}Cannot ping mariadb from backend${NC}"
echo ""

# 8. Check environment variables
echo -e "${YELLOW}8. Environment Variables in Backend:${NC}"
docker exec movie-booking-backend env | grep -E "SPRING|DB_|MYSQL" | sort
echo ""

# 9. Check .env file
echo -e "${YELLOW}9. Environment file (.env):${NC}"
if [ -f .env ]; then
    echo -e "${GREEN}.env file exists${NC}"
    echo "Variables (masked):"
    cat .env | grep -v "^#" | grep -v "^$" | sed 's/=.*/=***MASKED***/'
else
    echo -e "${RED}.env file not found!${NC}"
fi
echo ""

# 10. Resource usage
echo -e "${YELLOW}10. Container Resource Usage:${NC}"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" | grep movie-booking
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Debug information collected!${NC}"
echo -e "${BLUE}========================================${NC}"
