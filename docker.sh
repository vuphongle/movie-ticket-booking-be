#!/bin/bash

# Docker Helper Script - Movie Ticket Booking Backend

set -e

IMAGE_NAME="movie-ticket-booking-be"
CONTAINER_NAME="movie-booking-api"

print_usage() {
    echo "Usage: $0 [COMMAND]"
    echo
    echo "Commands:"
    echo "  build      Build Docker image"
    echo "  run        Run container"
    echo "  stop       Stop container"
    echo "  logs       Show logs"
    echo "  status     Show status"
    echo "  clean      Remove container and image"
    echo
}

case "${1:-help}" in
    build)
        echo "🔨 Building Docker image..."
        docker build -t ${IMAGE_NAME} .
        echo "✅ Build complete!"
        ;;
    run)
        if [ ! -f ".env" ]; then
            echo "⚠️  .env file not found. Creating from example..."
            cp .env.example .env
            echo "📝 Please edit .env with your settings"
        fi
        echo "🚀 Starting container..."
        docker stop ${CONTAINER_NAME} 2>/dev/null || true
        docker rm ${CONTAINER_NAME} 2>/dev/null || true
        docker run -d --name ${CONTAINER_NAME} -p 8080:8080 --env-file .env ${IMAGE_NAME}
        echo "✅ Container started at http://localhost:8080"
        ;;
    stop)
        echo "🛑 Stopping container..."
        docker stop ${CONTAINER_NAME} 2>/dev/null || true
        docker rm ${CONTAINER_NAME} 2>/dev/null || true
        echo "✅ Container stopped"
        ;;
    logs)
        docker logs -f ${CONTAINER_NAME}
        ;;
    status)
        echo "📊 Status:"
        docker ps -a --filter name=${CONTAINER_NAME} --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        echo
        docker images ${IMAGE_NAME} --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
        ;;
    clean)
        echo "🧹 Cleaning up..."
        docker stop ${CONTAINER_NAME} 2>/dev/null || true
        docker rm ${CONTAINER_NAME} 2>/dev/null || true
        docker rmi ${IMAGE_NAME} 2>/dev/null || true
        echo "✅ Cleanup complete"
        ;;
    help|*)
        print_usage
        ;;
esac
