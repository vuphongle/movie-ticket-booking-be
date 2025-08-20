#!/bin/bash

# Deploy script for Movie Ticket Booking API
# This script should be placed on your Digital Ocean droplet

set -e

echo "🚀 Starting deployment process..."

# Configuration
PROJECT_DIR="/opt/movie-booking"
BACKUP_DIR="/opt/movie-booking/backups"
COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env.prod"

# Create directories if they don't exist
mkdir -p $PROJECT_DIR
mkdir -p $BACKUP_DIR

# Change to project directory
cd $PROJECT_DIR

# Function to backup database
backup_database() {
    echo "📦 Creating database backup..."
    BACKUP_NAME="backup_$(date +%Y%m%d_%H%M%S).sql"
    docker exec movie-booking-db-prod mysqldump -u$DB_USERNAME -p$DB_PASSWORD $DB_NAME > $BACKUP_DIR/$BACKUP_NAME
    echo "✅ Database backup created: $BACKUP_NAME"
    
    # Keep only last 5 backups
    ls -t $BACKUP_DIR/backup_*.sql | tail -n +6 | xargs -r rm
}

# Function to health check
health_check() {
    echo "🏥 Performing health check..."
    sleep 30
    
    for i in {1..10}; do
        if curl -f http://localhost:8080/actuator/health; then
            echo "✅ Application is healthy!"
            return 0
        fi
        echo "⏳ Waiting for application to start... ($i/10)"
        sleep 10
    done
    
    echo "❌ Health check failed!"
    return 1
}

# Function to rollback
rollback() {
    echo "🔄 Rolling back to previous version..."
    docker-compose -f $COMPOSE_FILE down
    
    # Pull previous image tag (you might want to implement proper versioning)
    docker pull registry.digitalocean.com/$DO_REGISTRY_NAME/movie-ticket-booking-api:previous || true
    
    # Update docker-compose to use previous tag
    # This is a simple example - in practice, you'd want proper version management
    docker-compose -f $COMPOSE_FILE up -d
}

# Main deployment process
main() {
    echo "🔍 Checking prerequisites..."
    
    # Check if required files exist
    if [[ ! -f $ENV_FILE ]]; then
        echo "❌ Environment file $ENV_FILE not found!"
        exit 1
    fi
    
    # Load environment variables
    source $ENV_FILE
    
    # Login to DigitalOcean registry
    echo "🔐 Logging in to container registry..."
    docker login -u $DIGITALOCEAN_ACCESS_TOKEN -p $DIGITALOCEAN_ACCESS_TOKEN registry.digitalocean.com
    
    # Backup database if containers are running
    if docker container inspect movie-booking-db-prod >/dev/null 2>&1; then
        backup_database
    fi
    
    # Pull latest image
    echo "📥 Pulling latest application image..."
    docker pull registry.digitalocean.com/$DO_REGISTRY_NAME/movie-ticket-booking-api:latest
    
    # Stop existing containers
    echo "🛑 Stopping existing containers..."
    docker-compose -f $COMPOSE_FILE down || true
    
    # Start new containers
    echo "🚀 Starting new containers..."
    docker-compose -f $COMPOSE_FILE up -d
    
    # Health check
    if health_check; then
        echo "🎉 Deployment successful!"
        
        # Clean up old images
        echo "🧹 Cleaning up old Docker images..."
        docker image prune -f
        
        # Show running containers
        echo "📋 Running containers:"
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        
    else
        echo "❌ Deployment failed! Attempting rollback..."
        rollback
        exit 1
    fi
}

# Execute main function
main "$@"
