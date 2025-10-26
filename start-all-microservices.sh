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

# Function to start a service in its own terminal
start_service_in_terminal() {
    local service_name=$1
    local service_dir=$2
    local profile=$3
    
    print_status "Starting $service_name in new terminal..."
    
    # Detect terminal emulator
    local terminal_cmd=""
    if command -v gnome-terminal > /dev/null 2>&1; then
        terminal_cmd="gnome-terminal"
    elif command -v xterm > /dev/null 2>&1; then
        terminal_cmd="xterm"
    elif command -v konsole > /dev/null 2>&1; then
        terminal_cmd="konsole"
    elif command -v alacritty > /dev/null 2>&1; then
        terminal_cmd="alacritty"
    else
        print_error "No terminal emulator found. Please install gnome-terminal, xterm, konsole, or alacritty"
        return 1
    fi
    
    # Create terminal with service
    case $terminal_cmd in
        "gnome-terminal")
            gnome-terminal --title="$service_name" -- bash -c "cd $service_dir && echo '🚀 Starting $service_name...' && ./gradlew bootRun --args='--spring.profiles.active=$profile'; exec bash"
            ;;
        "xterm")
            xterm -title "$service_name" -e bash -c "cd $service_dir && echo '🚀 Starting $service_name...' && ./gradlew bootRun --args='--spring.profiles.active=$profile'; exec bash" &
            ;;
        "konsole")
            konsole --title "$service_name" -e bash -c "cd $service_dir && echo '🚀 Starting $service_name...' && ./gradlew bootRun --args='--spring.profiles.active=$profile'; exec bash" &
            ;;
        "alacritty")
            alacritty --title "$service_name" -e bash -c "cd $service_dir && echo '🚀 Starting $service_name...' && ./gradlew bootRun --args='--spring.profiles.active=$profile'; exec bash" &
            ;;
    esac
    
    # Wait a moment for terminal to open
    sleep 3
    
    print_success "$service_name terminal opened successfully"
}

# Create logs directory
mkdir -p logs

# Start services in separate terminals
start_service_in_terminal "Order Service" "server" "docker"
start_service_in_terminal "Inventory Service" "inventory-service" "docker"
start_service_in_terminal "Event Relay Service" "event-relay-service" "docker"

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
if curl -s http://localhost:8083/actuator/health > /dev/null 2>&1; then
    print_success "Event Relay Service is healthy"
else
    print_warning "Event Relay Service health check failed"
fi

print_success "🎉 All services started successfully!"
echo ""
echo "🌐 Service URLs:"
echo "  - Order Service:     http://localhost:8081"
echo "  - Inventory Service: http://localhost:8082"
echo "  - Event Relay Service: http://localhost:8083"
echo "  - Kafka UI:          http://localhost:8080"
echo "  - pgAdmin:           http://localhost:5050"
echo ""
echo "📊 Monitoring:"
echo "  - Each service runs in its own terminal window"
echo "  - Logs are visible in each terminal"
echo "  - Docker containers: docker compose ps"
echo ""
echo "🛑 To stop all services:"
echo "  ./stop-all-microservices.sh"
echo "  (or close the terminal windows manually)"
echo ""
echo "💡 Tips:"
echo "  - Each terminal shows real-time logs for that service"
echo "  - You can interact with each service independently"
echo "  - Close individual terminals to stop specific services"
