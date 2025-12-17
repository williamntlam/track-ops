#!/bin/bash

# TrackOps Docker Services Log Monitor
# This script opens separate terminal windows to monitor logs of running services

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

# Function to open log monitoring terminal
open_log_terminal() {
    local service_name=$1
    local terminal_cmd=$2
    
    local project_root="$(dirname "$0")/.."
    
    # Create a script that will run in the terminal
    local terminal_script="/tmp/trackops-logs-${service_name}.sh"
    
    cat > "$terminal_script" << EOF
#!/bin/bash
cd "$project_root"
echo "Monitoring logs for $service_name..."
echo "Press Ctrl+C to stop monitoring"
echo "----------------------------------------"

# Follow logs for the service
docker-compose logs -f "$service_name"

echo "Log monitoring stopped. Press any key to close this window..."
read -n 1
EOF
    
    chmod +x "$terminal_script"
    
    # Open terminal based on detected terminal emulator
    case "$terminal_cmd" in
        "gnome-terminal")
            gnome-terminal --title="TrackOps Logs - $service_name" -- bash -c "$terminal_script; exec bash"
            ;;
        "xterm")
            xterm -title "TrackOps Logs - $service_name" -e "$terminal_script" &
            ;;
        "konsole")
            konsole --title "TrackOps Logs - $service_name" -e bash -c "$terminal_script; exec bash" &
            ;;
        "xfce4-terminal")
            xfce4-terminal --title="TrackOps Logs - $service_name" -e "$terminal_script" &
            ;;
        "mate-terminal")
            mate-terminal --title="TrackOps Logs - $service_name" -e "$terminal_script" &
            ;;
        "lxterminal")
            lxterminal --title="TrackOps Logs - $service_name" -e "$terminal_script" &
            ;;
        *)
            print_error "No suitable terminal emulator found!"
            exit 1
            ;;
    esac
    
    # Wait a moment for the terminal to start
    sleep 1
}

# Main execution
print_status "Opening TrackOps Service Log Monitors..."
print_status "======================================="

# Change to project root directory
cd "$(dirname "$0")/.."

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed or not in PATH"
    exit 1
fi

# Detect terminal emulator
TERMINAL_CMD=$(detect_terminal)
if [ "$TERMINAL_CMD" = "none" ]; then
    print_error "No terminal emulator found. Please install one of the supported terminals."
    exit 1
fi

print_status "Using terminal emulator: $TERMINAL_CMD"

# Check if services are running
if ! docker-compose ps | grep -q "Up"; then
    print_warning "No services appear to be running. Please start services first with:"
    print_warning "  ./docker/start-all.sh"
    print_warning "  or"
    print_warning "  ./docker/start-all-terminals.sh"
    exit 1
fi

# Services to monitor
services=("postgres" "redis" "kafka" "prometheus" "grafana" "trackops-server")

# Open log monitoring terminals for each running service
for service in "${services[@]}"; do
    if docker-compose ps "$service" | grep -q "Up"; then
        print_status "Opening log monitor for $service..."
        open_log_terminal "$service" "$TERMINAL_CMD"
    else
        print_warning "$service is not running, skipping log monitor"
    fi
done

print_success "Log monitoring terminals opened!"
print_status "======================================="
print_status "Each terminal will show real-time logs for its service."
print_status "Close individual terminals to stop monitoring specific services."
