#!/bin/bash

# Restart Order Service Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Get the project root directory (parent of scripts folder)
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT/server"

print_status "Stopping Order Service..."
pkill -f "com.trackops.server.ServerApplication" 2>/dev/null || print_warning "No running Order Service found"
sleep 3

print_status "Starting Order Service..."
mkdir -p ../logs

# Start in background
nohup ./gradlew bootRun --args='--spring.profiles.active=default' > ../logs/Order\ Service.log 2>&1 &
SERVICE_PID=$!

print_success "Order Service started (PID: $SERVICE_PID)"
print_status "Waiting for service to be ready..."
sleep 10

# Check if service is healthy
if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    print_success "Order Service is healthy and running on port 8081"
    echo ""
    echo "📝 Logs are being written to: logs/Order Service.log"
    echo "🔍 View logs: tail -f logs/Order\\ Service.log"
    echo "🌐 Health check: curl http://localhost:8081/actuator/health"
else
    print_warning "Service started but health check not yet passing"
    print_status "Check logs: tail -f logs/Order\\ Service.log"
fi

