#!/bin/bash

# TrackOps Docker Services Status Script
# This script shows the status of all services

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Main execution
print_status "TrackOps Docker Services Status"
print_status "==============================="

# Change to project root directory
cd "$(dirname "$0")/.."

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed or not in PATH"
    exit 1
fi

# Show container status
print_status "Container Status:"
docker-compose ps

echo ""

# Show service health
print_status "Service Health Check:"
services=("postgres" "redis" "kafka" "prometheus" "grafana" "trackops-server")

for service in "${services[@]}"; do
    if docker-compose ps $service | grep -q "healthy"; then
        print_success "$service: Healthy"
    elif docker-compose ps $service | grep -q "unhealthy"; then
        print_error "$service: Unhealthy"
    elif docker-compose ps $service | grep -q "Up"; then
        print_warning "$service: Running (no health check)"
    else
        print_error "$service: Not running"
    fi
done

echo ""

# Show service URLs
print_status "Service URLs:"
print_status "  TrackOps Server: http://localhost:8080"
print_status "  Grafana:         http://localhost:3000 (admin/admin)"
print_status "  Prometheus:      http://localhost:9090"
print_status "  PostgreSQL:      localhost:5432"
print_status "  Redis:           localhost:6379"
print_status "  Kafka:           localhost:9092"

echo ""

# Show resource usage
print_status "Resource Usage:"
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}" 2>/dev/null || print_warning "Could not retrieve resource usage"
