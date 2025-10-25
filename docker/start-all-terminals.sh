#!/bin/bash

# TrackOps Docker Services Startup Script with Separate Terminals
# This script starts all services in separate terminal windows for better monitoring

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

# Function to detect available terminal emulator
detect_terminal() {
    if command -v gnome-terminal &> /dev/null; then
        echo "gnome-terminal"
    elif command -v xterm &> /dev/null; then
        echo "xterm"
    elif command -v konsole &> /dev/null; then
        echo "konsole"
    elif command -v xfce4-terminal &> /dev/null; then
        echo "xfce4-terminal"
    elif command -v mate-terminal &> /dev/null; then
        echo "mate-terminal"
    elif command -v lxterminal &> /dev/null; then
        echo "lxterminal"
    else
        echo "none"
    fi
}

# Function to open terminal with service
open_service_terminal() {
    local service_name=$1
    local service_file=$2
    local terminal_cmd=$3
    
    local project_root="$(dirname "$0")/.."
    local log_file="/tmp/trackops-${service_name}.log"
    
    # Create a script that will run in the terminal
    local terminal_script="/tmp/trackops-${service_name}-terminal.sh"
    
    cat > "$terminal_script" << EOF
#!/bin/bash
cd "$project_root"
echo "Starting $service_name..."
echo "Logs will be saved to: $log_file"
echo "Press Ctrl+C to stop this service"
echo "----------------------------------------"

# Start the service and follow logs
if [ -n "$service_file" ]; then
    docker-compose -f "$service_file" up 2>&1 | tee "$log_file"
else
    docker-compose up "$service_name" 2>&1 | tee "$log_file"
fi

echo "Service $service_name stopped. Press any key to close this window..."
read -n 1
EOF
    
    chmod +x "$terminal_script"
    
    # Open terminal based on detected terminal emulator
    case "$terminal_cmd" in
        "gnome-terminal")
            gnome-terminal --title="TrackOps - $service_name" -- bash -c "$terminal_script; exec bash"
            ;;
        "xterm")
            xterm -title "TrackOps - $service_name" -e "$terminal_script" &
            ;;
        "konsole")
            konsole --title "TrackOps - $service_name" -e bash -c "$terminal_script; exec bash" &
            ;;
        "xfce4-terminal")
            xfce4-terminal --title="TrackOps - $service_name" -e "$terminal_script" &
            ;;
        "mate-terminal")
            mate-terminal --title="TrackOps - $service_name" -e "$terminal_script" &
            ;;
        "lxterminal")
            lxterminal --title="TrackOps - $service_name" -e "$terminal_script" &
            ;;
        *)
            print_error "No suitable terminal emulator found!"
            print_error "Please install one of: gnome-terminal, xterm, konsole, xfce4-terminal, mate-terminal, lxterminal"
            exit 1
            ;;
    esac
    
    # Wait a moment for the terminal to start
    sleep 2
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

# Main execution
print_status "Starting TrackOps Docker Services in Separate Terminals..."
print_status "=========================================================="

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

# Detect terminal emulator
TERMINAL_CMD=$(detect_terminal)
if [ "$TERMINAL_CMD" = "none" ]; then
    print_error "No terminal emulator found. Please install one of the supported terminals."
    exit 1
fi

print_status "Using terminal emulator: $TERMINAL_CMD"

# Stop any existing containers first
print_status "Stopping any existing containers..."
docker-compose down 2>/dev/null || true

# Start infrastructure services first (in dependency order)
print_status "Phase 1: Starting infrastructure services in separate terminals..."

# 1. Start PostgreSQL
print_status "Opening PostgreSQL terminal..."
open_service_terminal "postgres" "docker/postgres.yml" "$TERMINAL_CMD"
if ! check_service_health "postgres"; then
    print_error "Failed to start PostgreSQL properly"
    exit 1
fi

# 2. Start Redis
print_status "Opening Redis terminal..."
open_service_terminal "redis" "docker/redis.yml" "$TERMINAL_CMD"
if ! check_service_health "redis"; then
    print_error "Failed to start Redis properly"
    exit 1
fi

# 3. Start Zookeeper
print_status "Opening Zookeeper terminal..."
open_service_terminal "zookeeper" "docker/zookeeper.yml" "$TERMINAL_CMD"
if ! check_service_health "zookeeper"; then
    print_error "Failed to start Zookeeper properly"
    exit 1
fi

# 4. Start Kafka (depends on Zookeeper)
print_status "Opening Kafka terminal..."
open_service_terminal "kafka" "docker/kafka.yml" "$TERMINAL_CMD"
if ! check_service_health "kafka"; then
    print_error "Failed to start Kafka properly"
    exit 1
fi

# 5. Start Prometheus
print_status "Opening Prometheus terminal..."
open_service_terminal "prometheus" "docker/prometheus.yml" "$TERMINAL_CMD"
if ! check_service_health "prometheus"; then
    print_error "Failed to start Prometheus properly"
    exit 1
fi

# 6. Start Grafana
print_status "Opening Grafana terminal..."
open_service_terminal "grafana" "docker/grafana.yml" "$TERMINAL_CMD"
if ! check_service_health "grafana"; then
    print_error "Failed to start Grafana properly"
    exit 1
fi

print_success "All infrastructure services are running and healthy!"

# Start the main application last
print_status "Phase 2: Starting TrackOps Spring Boot application..."
print_status "Opening TrackOps Server terminal..."
open_service_terminal "trackops-server" "docker/trackops-server.yml" "$TERMINAL_CMD"
if ! check_service_health "trackops-server"; then
    print_error "Failed to start TrackOps Server properly"
    exit 1
fi

print_success "All services are running successfully in separate terminals!"
print_status "=========================================================="
print_status "Service URLs:"
print_status "  TrackOps Server: http://localhost:8080"
print_status "  Grafana:         http://localhost:3000 (admin/admin)"
print_status "  Prometheus:      http://localhost:9090"
print_status "  PostgreSQL:      localhost:5432"
print_status "  Redis:           localhost:6379"
print_status "  Kafka:           localhost:9092"
print_status "=========================================================="
print_status "Each service is running in its own terminal window."
print_status "You can monitor logs and stop individual services by closing their terminals."
print_status "Log files are also saved to /tmp/trackops-*.log"

# Show running containers
print_status "Running containers:"
docker-compose ps
