# Docker Services Configuration

This directory contains individual Docker Compose service definitions for the TrackOps application. Each service is defined in its own file for better organization and maintainability.

## Service Files

- `postgres.yml` - PostgreSQL database configuration
- `redis.yml` - Redis cache configuration  
- `kafka.yml` - Kafka message broker configuration (KRaft mode - no ZooKeeper required)
- `prometheus.yml` - Prometheus monitoring configuration
- `grafana.yml` - Grafana dashboards configuration
- `trackops-server.yml` - TrackOps Spring Boot application configuration

## Usage

### Quick Start Scripts

The easiest way to manage all services is using the provided scripts:

```bash
# Start all services in correct order with health checks (single terminal)
./docker/start-all.sh

# Start all services in separate terminal windows (better for monitoring)
./docker/start-all-terminals.sh

# Check status of all services
./docker/status.sh

# Stop all services
./docker/stop-all.sh

# Monitor logs in separate terminals (for already running services)
./docker/monitor-logs.sh
```

### Manual Docker Compose Usage

The main `docker-compose.yml` file in the project root includes all these services. You can:

1. **Start all services**: `docker-compose up -d`
2. **Start specific services**: `docker-compose up -d postgres redis`
3. **View logs**: `docker-compose logs -f [service-name]`
4. **Stop services**: `docker-compose down`

### Individual Service Management

You can also run individual service files directly:

```bash
# Start only PostgreSQL
docker-compose -f docker/postgres.yml up -d

# Start only Redis
docker-compose -f docker/redis.yml up -d
```

## Startup Script Features

### `start-all.sh` (Single Terminal)
- **Dependency Management**: Starts services in the correct order
- **Health Checks**: Waits for each service to be healthy before starting the next
- **Error Handling**: Stops execution if any service fails to start
- **Colored Output**: Easy-to-read status messages
- **Service URLs**: Shows all available service endpoints after startup

### `start-all-terminals.sh` (Multiple Terminals)
- **All features of start-all.sh** plus:
- **Separate Terminal Windows**: Each service runs in its own terminal
- **Real-time Log Monitoring**: See logs from each service as they start
- **Individual Service Control**: Stop services by closing their terminals
- **Log File Backup**: Logs are also saved to `/tmp/trackops-*.log`
- **Auto-detection**: Automatically detects available terminal emulators

### `monitor-logs.sh` (Log Monitoring Only)
- **Log Monitoring**: Opens terminals to monitor logs of already running services
- **Real-time Updates**: Follow logs in real-time
- **Service Detection**: Only opens monitors for services that are actually running

### Supported Terminal Emulators
- GNOME Terminal
- XTerm
- Konsole (KDE)
- XFCE Terminal
- MATE Terminal
- LXTerminal

## Ports

- PostgreSQL: 5432
- Redis: 6379
- Kafka: 9092 (broker), 9093 (controller)
- Prometheus: 9090
- Grafana: 3000
- TrackOps Server: 8080
