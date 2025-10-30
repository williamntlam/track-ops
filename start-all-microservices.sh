#!/bin/bash

# TrackOps Complete Startup Script
# Starts infrastructure and all microservices

set -e  # Exit on any error

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
./start-infrastructure.sh

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
    docker compose stop kafka >/dev/null 2>&1 || true
    docker compose rm -f -s kafka >/dev/null 2>&1 || true

    print_status "Removing Kafka data volume..."
    docker volume rm track-ops_kafka_data >/dev/null 2>&1 || true

    # Ensure ZooKeeper is running first
    print_status "Ensuring ZooKeeper is running..."
    docker compose up -d zookeeper >/dev/null 2>&1
    sleep 2

    print_status "Starting Kafka fresh..."
    docker compose up -d kafka

    # Wait briefly for Kafka to initialize
    sleep 5
}

# Ensure Kafka is healthy (and self-heal if needed) before starting Debezium Connect
ensure_kafka_healthy

docker compose up -d debezium-connect

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

# Function to start a service in background
start_service_background() {
    local service_name=$1
    local service_dir=$2
    local profile=$3
    
    print_status "Starting $service_name in background..."
    
    # Create logs directory
    mkdir -p logs
    
    # Start service in background
    cd "$service_dir"
    if [ "$profile" = "default" ] || [ -z "$profile" ]; then
        nohup ./gradlew bootRun > "../logs/${service_name}.log" 2>&1 &
    else
        nohup ./gradlew bootRun --args="--spring.profiles.active=$profile" > "../logs/${service_name}.log" 2>&1 &
    fi
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

# Wait for services to be ready
print_status "Waiting for services to be ready..."
sleep 10


# Check service health
print_status "Checking service health..."

# Check Order Service
if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    print_success "Order Service is healthy"
else
    print_warning "Order Service health check failed"
fi

# Check Inventory Service
if curl -s http://localhost:8082/actuator/health > /dev/null 2>&1; then
    print_success "Inventory Service is healthy"
else
    print_warning "Inventory Service health check failed"
fi

# Check Event Relay Service
if curl -s http://localhost:8084/actuator/health > /dev/null 2>&1; then
    print_success "Event Relay Service is healthy"
else
    print_warning "Event Relay Service health check failed"
fi

print_success "🎉 All services started successfully!"
echo ""
echo "🌐 Service URLs:"
echo "  - Order Service:     http://localhost:8081"
echo "  - Inventory Service: http://localhost:8082"
echo "  - Event Relay Service: http://localhost:8084"
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
echo "  ./stop-all-microservices.sh"
echo ""
echo "📋 To view logs:"
echo "  tail -f logs/Order\\ Service.log"
echo "  tail -f logs/Inventory\\ Service.log"
echo "  tail -f logs/Event\\ Relay\\ Service.log"
echo ""
echo "💡 Tips:"
echo "  - All services run in background processes"
echo "  - Logs are saved to ./logs/ directory"
echo "  - Use ./stop-all-microservices.sh to stop everything"
