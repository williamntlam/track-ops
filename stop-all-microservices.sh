#!/bin/bash

# TrackOps Complete Shutdown Script
# Stops all microservices and infrastructure

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

print_status "🛑 Stopping TrackOps Complete System..."

# Function to stop a service (for terminal-based services)
stop_service() {
    local service_name=$1
    local process_pattern="gradlew.*bootRun.*${service_name,,}"
    
    print_status "Stopping $service_name..."
    
    # Find and kill gradlew processes for this service
    local pids=$(pgrep -f "$process_pattern" 2>/dev/null || true)
    
    if [ -n "$pids" ]; then
        echo "$pids" | xargs kill 2>/dev/null || true
        sleep 2
        
        # Force kill if still running
        local remaining_pids=$(pgrep -f "$process_pattern" 2>/dev/null || true)
        if [ -n "$remaining_pids" ]; then
            print_warning "Force stopping $service_name..."
            echo "$remaining_pids" | xargs kill -9 2>/dev/null || true
        fi
        
        print_success "$service_name stopped"
    else
        print_warning "$service_name was not running"
    fi
}

# Stop microservices
print_status "Phase 1: Stopping Microservices..."
stop_service "Order Service"
stop_service "Inventory Service"
stop_service "Event Relay Service"

# Stop infrastructure
print_status "Phase 2: Stopping Infrastructure Services..."
docker compose down

print_success "🎉 All services stopped successfully!"
echo ""
echo "📊 Cleanup:"
echo "  - Terminal windows closed"
echo "  - Docker volumes preserved"
echo "  - Database data preserved"
echo ""
echo "🚀 To start again:"
echo "  ./start-all-microservices.sh"
echo ""
echo "💡 Note:"
echo "  - Services now run in separate terminal windows"
echo "  - You can also close terminals manually to stop services"
