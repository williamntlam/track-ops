#!/bin/bash

# TrackOps Complete Shutdown Script
# Stops all microservices and infrastructure

set -e  # Exit on any error

# Change to project root directory
cd "$(dirname "$0")/../.."

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

print_status "🛑 Stopping TrackOps Complete System..."

# Function to stop a service by main class name
stop_service_by_class() {
    local service_name=$1
    local main_class=$2
    local port=$3
    
    print_status "Stopping $service_name (port $port)..."
    
    # Find processes by main class name
    local pids=$(pgrep -f "$main_class" 2>/dev/null || true)
    
    if [ -n "$pids" ]; then
        echo "$pids" | xargs kill 2>/dev/null || true
        sleep 3
        
        # Force kill if still running
        local remaining_pids=$(pgrep -f "$main_class" 2>/dev/null || true)
        if [ -n "$remaining_pids" ]; then
            print_warning "Force stopping $service_name..."
            echo "$remaining_pids" | xargs kill -9 2>/dev/null || true
        fi
        
        print_success "$service_name stopped"
    else
        print_warning "$service_name was not running"
    fi
}

# Function to stop a service by port
stop_service_by_port() {
    local service_name=$1
    local port=$2
    
    print_status "Checking $service_name on port $port..."
    
    # Check if port is in use
    if ss -tulpn | grep -q ":$port "; then
        local pid=$(ss -tulpn | grep ":$port " | awk '{print $6}' | sed 's/.*pid=\([0-9]*\).*/\1/' | head -1)
        if [ -n "$pid" ]; then
            print_status "Stopping $service_name (PID: $pid)..."
            kill $pid 2>/dev/null || true
            sleep 2
            
            # Force kill if still running
            if kill -0 $pid 2>/dev/null; then
                print_warning "Force stopping $service_name..."
                kill -9 $pid 2>/dev/null || true
            fi
            
            print_success "$service_name stopped"
        fi
    else
        print_warning "$service_name was not running on port $port"
    fi
}

# Stop microservices
print_status "Phase 1: Stopping Microservices..."

# Check if services are running in Docker
if docker ps --format "{{.Names}}" | grep -q "trackops-inventory-service\|trackops-event-relay-service\|trackops-server"; then
    print_status "Stopping Docker containers..."
    docker compose -f docker/services/trackops-server.yml down 2>/dev/null || true
    docker compose -f docker/services/inventory-service.yml down 2>/dev/null || true
    docker compose -f docker/services/event-relay-service.yml down 2>/dev/null || true
    print_success "Docker containers stopped"
fi

# Stop Order Service (port 8081) - if running locally via Gradle
stop_service_by_class "Order Service" "ServerApplication" "8081"

# Stop Inventory Service (port 8082) - if running locally via Gradle
stop_service_by_class "Inventory Service" "InventoryServiceApplication" "8082"

# Stop Event Relay Service (port 8084) - if running locally via Gradle
stop_service_by_class "Event Relay Service" "EventRelayServiceApplication" "8084"

# Additional cleanup for any remaining gradlew processes
print_status "Cleaning up any remaining gradlew processes..."
pkill -f "gradlew.*bootRun" 2>/dev/null || true

# Stop infrastructure
print_status "Phase 2: Stopping Infrastructure Services..."

# Stop Debezium Connect first (depends on Kafka)
print_status "Stopping Debezium Connect..."
docker compose -f docker/services/debezium-connect.yml down 2>/dev/null || true

# Stop Kafka and Schema Registry
print_status "Stopping Kafka and Schema Registry..."
docker compose -f docker/services/kafka.yml down 2>/dev/null || true

# Stop Redis
print_status "Stopping Redis..."
docker compose -f docker/services/redis.yml down 2>/dev/null || true

# Stop PostgreSQL services
print_status "Stopping PostgreSQL services..."
docker compose -f docker/services/postgres.yml down 2>/dev/null || true

print_success "Infrastructure services stopped"

print_success "🎉 All services stopped successfully!"
echo ""
echo "📊 Cleanup Summary:"
echo "  - Order Service (port 8081) stopped"
echo "  - Inventory Service (port 8082) stopped" 
echo "  - Event Relay Service (port 8084) stopped"
echo "  - Debezium Connect (port 8083) stopped"
echo "  - Kafka and Schema Registry (port 8085) stopped"
echo "  - Redis (port 6379) stopped"
echo "  - PostgreSQL services stopped"
echo "  - Docker volumes preserved"
echo "  - Database data preserved"
echo ""
echo "🚀 To start again:"
echo "  ./scripts/infra/start-all-microservices.sh"
echo ""
echo "💡 Note:"
echo "  - Services run in background in single terminal"
echo "  - All ports are properly released"
echo "  - Use 'docker ps' to verify all services are stopped"
echo "  - Use 'USE_DOCKER=true ./scripts/infra/start-all-microservices.sh' for Docker mode"
