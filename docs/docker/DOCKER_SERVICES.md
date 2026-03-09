# Docker Services Configuration

This directory contains individual Docker Compose service definitions for the TrackOps application. Each service is defined in its own file for better organization and maintainability.

## Service Files

All Docker Compose service definitions now live under `docker/services`:

- `docker/services/postgres.yml` - PostgreSQL database configuration
- `docker/services/redis.yml` - Redis cache configuration  
- `docker/services/kafka.yml` - Kafka message broker configuration (KRaft mode - no ZooKeeper required)
- `docker/services/kafka-target.yml` - Target Kafka cluster for MirrorMaker 2 (cross-cluster replication / DR)
- `docker/services/mirror-maker-2.yml` - MirrorMaker 2.0 Connect worker (replicates source → target cluster). See [MIRRORMAKER2.md](../architecture/MIRRORMAKER2.md).
- `docker/services/debezium-connect.yml` - Debezium Connect worker
- `docker/services/prometheus.yml` - Prometheus monitoring configuration
- `docker/services/prometheus-local.yml` - Prometheus scrape configuration
- `docker/services/grafana.yml` - Grafana dashboards configuration
- `docker/services/trackops-server.yml` - TrackOps Spring Boot application configuration
- `docker/services/inventory-service.yml` - Inventory Service container
- `docker/services/event-relay-service.yml` - Event Relay Service container

## Usage

### Quick Start Scripts

The easiest way to manage all services is using the provided scripts:

```bash
# Start all services in correct order with health checks (single terminal)
./scripts/docker/start-all.sh

# Start all services in separate terminal windows (better for monitoring)
./scripts/docker/start-all-terminals.sh

# Check status of all services
./scripts/docker/status.sh

# Stop all services
./scripts/docker/stop-all.sh

# Monitor logs in separate terminals (for already running services)
./scripts/docker/monitor-logs.sh
```

### Individual Service Management

You can also run individual service files directly from the project root:

```bash
# Start only PostgreSQL
docker compose -f docker/services/postgres.yml up -d

# Start only Redis
docker compose -f docker/services/redis.yml up -d
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
- TrackOps Server: 8081
