#!/bin/bash

# TrackOps Complete Startup Script
# Starts infrastructure and all microservices

set -e  # Exit on any error

# Change to project root directory
cd "$(dirname "$0")/.."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
print_status() {
    echo -e "${BLUE}📋 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi

print_status "🚀 Starting TrackOps Complete System..."

# Step 1: Start Infrastructure
print_status "Phase 1: Starting Infrastructure Services..."
./scripts/start-infrastructure.sh

if [ $? -ne 0 ]; then
    print_error "Failed to start infrastructure services"
    exit 1
fi

print_success "Infrastructure services are ready!"

# Step 2: Start Debezium Connect (if using Debezium strategy)
print_status "Phase 2: Starting Debezium Connect..."

ensure_kafka_healthy() {
    print_status "Checking Kafka health before starting Debezium Connect..."

    local attempts=40
    local i
    for i in $(seq 1 $attempts); do
        KAFKA_HEALTH=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}unknown{{end}}' trackops-kafka 2>/dev/null || echo "unknown")
        if [ "$KAFKA_HEALTH" = "healthy" ]; then
            print_success "Kafka is healthy!"
            return 0
        fi
        # Check for cluster ID mismatch in recent logs
        if docker compose logs --no-color --tail=200 kafka 2>/dev/null | grep -q "InconsistentClusterIdException"; then
            print_warning "Detected Kafka InconsistentClusterIdException. Attempting self-heal (reset data volume)."
            reset_kafka_cluster
            # After reset, re-check health from the start
            i=0
        else
            print_status "Attempt $i/$attempts: Kafka not healthy yet (status: $KAFKA_HEALTH)..."
            sleep 3
        fi
    done

    print_warning "Kafka did not report healthy within the expected time. Proceeding anyway."
    return 1
}

reset_kafka_cluster() {
    print_status "Stopping Kafka to reset data..."
    docker compose -f docker/kafka.yml stop kafka >/dev/null 2>&1 || true
    docker compose -f docker/kafka.yml rm -f -s kafka >/dev/null 2>&1 || true

    print_status "Removing Kafka data volume..."
    docker volume rm track-ops_kafka_data >/dev/null 2>&1 || true

    print_status "Starting Kafka fresh (KRaft mode)..."
    docker compose -f docker/kafka.yml up -d kafka

    # Wait briefly for Kafka to initialize
    sleep 5
}

# Ensure Kafka is healthy (and self-heal if needed) before starting Debezium Connect
ensure_kafka_healthy

# Start Debezium Connect
print_status "Starting Debezium Connect..."
docker compose -f docker/debezium-connect.yml up -d

# Wait for Debezium Connect to be ready
print_status "Waiting for Debezium Connect to be ready..."
for i in {1..30}; do
    if curl -s http://localhost:8083/connectors > /dev/null 2>&1; then
        print_success "Debezium Connect is ready!"
        break
    fi
    print_status "Attempt $i/30: Debezium Connect not ready yet..."
    sleep 2
done

# Step 3: Setup Debezium Connectors
print_status "Phase 3: Setting up Debezium Connectors..."
if [ -f "./debezium-connectors/setup-debezium-connectors.sh" ]; then
    ./debezium-connectors/setup-debezium-connectors.sh
    print_success "Debezium connectors configured!"
else
    print_warning "Debezium setup script not found, skipping..."
fi

# Step 4: Start Microservices
print_status "Phase 4: Starting Microservices..."

# Check if USE_DOCKER environment variable is set
USE_DOCKER=${USE_DOCKER:-false}

if [ "$USE_DOCKER" = "true" ]; then
    print_status "Starting microservices via Docker Compose..."
    
    # Function to start a service via Docker Compose
    start_service_docker() {
        local service_name=$1
        local compose_file=$2
        
        print_status "Starting $service_name via Docker..."
        docker compose -f "$compose_file" up -d
        
        if [ $? -eq 0 ]; then
            print_success "$service_name container started"
        else
            print_error "$service_name failed to start"
            return 1
        fi
    }
    
    # Start services via Docker Compose
    start_service_docker "Order Service" "docker/trackops-server.yml"
    start_service_docker "Inventory Service" "docker/inventory-service.yml"
    start_service_docker "Event Relay Service" "docker/event-relay-service.yml"
    
else
    # Function to start a service in background (Gradle)
    start_service_background() {
        local service_name=$1
        local service_dir=$2
        local profile=$3
        
        print_status "Starting $service_name in background..."
        
        # Create logs directory
        mkdir -p logs
        
        # Start service in background
        cd "$service_dir"
        nohup ./gradlew bootRun --args="--spring.profiles.active=$profile" > "../logs/${service_name}.log" 2>&1 &
        local pid=$!
        echo $pid > "../logs/${service_name}.pid"
        
        cd ..
        
        # Wait a moment for startup
        sleep 5
        # Check if process is still running
        if kill -0 $pid 2>/dev/null; then
            print_success "$service_name started successfully (PID: $pid)"
        else
            print_error "$service_name failed to start"
            return 1
        fi
    }
    
    # Create logs directory
    mkdir -p logs
    
    # Start services in background
    start_service_background "Order Service" "server" "default"
    start_service_background "Inventory Service" "inventory-service" "default"
    start_service_background "Event Relay Service" "event-relay-service" "default"
fi

# Function to wait for service health with retries
wait_for_service_health() {
    local service_name=$1
    local url=$2
    local max_attempts=${3:-30}
    local attempt=1
    
    print_status "Waiting for $service_name to be healthy..."
    
    while [ $attempt -le $max_attempts ]; do
        # Use curl -f to fail on HTTP error codes (4xx, 5xx)
        # Check if we get a successful HTTP response
        if curl -s -f "$url" > /dev/null 2>&1; then
            print_success "$service_name is healthy"
            return 0
        fi
        
        if [ $attempt -lt $max_attempts ]; then
            echo -n "."
            sleep 2
        fi
        attempt=$((attempt + 1))
    done
    
    print_warning "$service_name health check failed after $max_attempts attempts"
    print_warning "  Service may still be starting. Check logs: tail -f logs/${service_name}.log"
    return 1
}

# Wait for services to be ready
print_status "Waiting for services to be ready..."
print_status "This may take 30-60 seconds for services to fully start..."

# Check service health with retries
print_status "Checking service health..."

# Check Order Service
wait_for_service_health "Order Service" "http://localhost:8081/actuator/health" 30

# Check Inventory Service
wait_for_service_health "Inventory Service" "http://localhost:8082/actuator/health" 30

# Event Relay Service Configuration
EVENT_RELAY_SERVICE_HOST=${EVENT_RELAY_SERVICE_HOST:-localhost}
EVENT_RELAY_SERVICE_PORT=${EVENT_RELAY_SERVICE_PORT:-8084}
EVENT_RELAY_SERVICE_URL="http://${EVENT_RELAY_SERVICE_HOST}:${EVENT_RELAY_SERVICE_PORT}/actuator/health"

# Check Event Relay Service
wait_for_service_health "Event Relay Service" "$EVENT_RELAY_SERVICE_URL" 30

print_success "🎉 All services started successfully!"
echo ""
echo "🌐 Service URLs:"
echo "  - Order Service:     http://localhost:8081"
echo "  - Inventory Service: http://localhost:8082"
echo "  - Event Relay Service: http://${EVENT_RELAY_SERVICE_HOST}:${EVENT_RELAY_SERVICE_PORT}"
echo "  - Debezium Connect: http://localhost:8083"
echo "  - Kafka UI:          http://localhost:8080"
echo "  - pgAdmin:           http://localhost:5050"
echo ""
echo "📊 Monitoring:"
echo "  - All services run in background"
echo "  - Service logs: ./logs/"
echo "  - Service PIDs: ./logs/*.pid"
echo "  - Docker containers: docker compose ps"
echo ""
echo "🛑 To stop all services:"
echo "  ./scripts/stop-all-microservices.sh"
echo ""
echo "📋 To view logs:"
echo "  tail -f logs/Order\\ Service.log"
echo "  tail -f logs/Inventory\\ Service.log"
echo "  tail -f logs/Event\\ Relay\\ Service.log"
echo ""
echo "💡 Tips:"
if [ "$USE_DOCKER" = "true" ]; then
    echo "  - All services run in Docker containers"
    echo "  - View logs: docker logs trackops-server"
    echo "  - View logs: docker logs trackops-inventory-service"
    echo "  - View logs: docker logs trackops-event-relay-service"
else
    echo "  - All services run in background processes"
    echo "  - Logs are saved to ./logs/ directory"
fi
echo "  - Use ./scripts/stop-all-microservices.sh to stop everything"
echo ""
echo "🐳 To start services via Docker instead of Gradle:"
echo "  USE_DOCKER=true ./scripts/start-all-microservices.sh"
