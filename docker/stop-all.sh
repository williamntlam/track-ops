#!/bin/bash

# TrackOps Docker Services Stop Script
# This script stops all services gracefully

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

# Main execution
print_status "Stopping TrackOps Docker Services..."
print_status "===================================="

# Change to project root directory
cd "$(dirname "$0")/.."

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed or not in PATH"
    exit 1
fi

# Stop all services
print_status "Stopping all containers..."
docker-compose down

# Optional: Remove volumes (uncomment if you want to clean data)
# print_warning "Removing volumes (this will delete all data)..."
# docker-compose down -v

# Optional: Remove images (uncomment if you want to clean images)
# print_warning "Removing images..."
# docker-compose down --rmi all

print_success "All services have been stopped!"
print_status "===================================="

# Show remaining containers
print_status "Remaining containers:"
docker-compose ps 2>/dev/null || print_status "No containers running"
