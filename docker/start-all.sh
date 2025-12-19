#!/bin/bash

# TrackOps Docker Services Startup Script
# This script starts all services in the correct order with health checks

set -e  # Exit on any error

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

# Function to check if a service is healthy
check_service_health() {
    local service_name=$1
    local max_attempts=30
    local attempt=1
    
    print_status "Waiting for $service_name to be healthy..."
    
    while [ $attempt -le $max_attempts ]; do
        if docker-compose ps $service_name | grep -q "healthy"; then
            print_success "$service_name is healthy!"
            return 0
        elif docker-compose ps $service_name | grep -q "unhealthy"; then
            print_error "$service_name is unhealthy!"
            return 1
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_error "$service_name failed to become healthy within $((max_attempts * 2)) seconds"
    return 1
}

# Function to start a service and wait for it to be healthy
start_service() {
    local service_name=$1
    local service_file=$2
    
    print_status "Starting $service_name..."
    
    if [ -n "$service_file" ]; then
        docker-compose -f "$service_file" up -d
    else
        docker-compose up -d "$service_name"
    fi
    
    if ! check_service_health "$service_name"; then
        print_error "Failed to start $service_name properly"
        exit 1
    fi
}

# Main execution
print_status "Starting TrackOps Docker Services..."
print_status "======================================"

# Change to project root directory
cd "$(dirname "$0")/.."

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed or not in PATH"
    exit 1
fi

# Check if Docker is running
if ! docker info &> /dev/null; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi

# Stop any existing containers first
print_status "Stopping any existing containers..."
docker-compose down 2>/dev/null || true

# Start infrastructure services first (in dependency order)
print_status "Phase 1: Starting infrastructure services..."

# 1. Start PostgreSQL
start_service "postgres" "docker/postgres.yml"

# 2. Start Redis
start_service "redis" "docker/redis.yml"

# 3. Start Kafka (KRaft mode - no ZooKeeper required)
start_service "kafka" "docker/kafka.yml"

# 5. Start Prometheus
start_service "prometheus" "docker/prometheus.yml"

# 6. Start Grafana
start_service "grafana" "docker/grafana.yml"

print_success "All infrastructure services are running and healthy!"

# Start microservices
print_status "Phase 2: Starting TrackOps Microservices..."

# Start Order Service
start_service "trackops-server" "docker/trackops-server.yml"

# Start Inventory Service
print_status "Starting Inventory Service..."
docker compose -f docker/inventory-service.yml up -d
sleep 10
if docker ps --filter "name=trackops-inventory-service" --format "{{.Status}}" | grep -q "healthy"; then
    print_success "Inventory Service is healthy!"
elif docker ps --filter "name=trackops-inventory-service" --format "{{.Status}}" | grep -q "Up"; then
    print_warning "Inventory Service is running (health check pending)"
else
    print_error "Inventory Service failed to start"
fi

# Start Event Relay Service
print_status "Starting Event Relay Service..."
docker compose -f docker/event-relay-service.yml up -d
sleep 10
if docker ps --filter "name=trackops-event-relay-service" --format "{{.Status}}" | grep -q "healthy"; then
    print_success "Event Relay Service is healthy!"
elif docker ps --filter "name=trackops-event-relay-service" --format "{{.Status}}" | grep -q "Up"; then
    print_warning "Event Relay Service is running (health check pending)"
else
    print_error "Event Relay Service failed to start"
fi

print_success "All services are running successfully!"
print_status "======================================"
print_status "Service URLs:"
print_status "  TrackOps Server:     http://localhost:8081"
print_status "  Inventory Service:   http://localhost:8082"
print_status "  Event Relay Service:  http://localhost:8084"
print_status "  Schema Registry:     http://localhost:8081"
print_status "  Grafana:              http://localhost:3000 (admin/admin)"
print_status "  Prometheus:           http://localhost:9090"
print_status "  PostgreSQL:           localhost:5432"
print_status "  Redis:                localhost:6379"
print_status "  Kafka:                 localhost:9092"
print_status "======================================"

# Show running containers
print_status "Running containers:"
docker-compose ps
