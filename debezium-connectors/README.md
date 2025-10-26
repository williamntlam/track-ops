# Debezium Connector Configuration

This directory contains Debezium connector configurations for TrackOps.

## 📁 Files

- `trackops-orders-connector.json` - PostgreSQL connector for orders database
- `setup-debezium-connectors.sh` - Script to create connectors
- `README.md` - This documentation

## 🚀 Quick Start

### 1. Start Infrastructure with Debezium
```bash
# Start all services including Debezium
./start-infrastructure.sh
```

### 2. Setup Debezium Connectors
```bash
# Create the connectors
./debezium-connectors/setup-debezium-connectors.sh
```

### 3. Configure Application for Debezium
```bash
# Start server with Debezium strategy
cd server
./gradlew bootRun --args='--app.event-publishing.strategy=debezium'
```

## 🔧 Connector Details

### TrackOps Orders Connector
- **Name**: `trackops-orders-connector`
- **Database**: `trackops_orders`
- **Tables**: `orders`, `order_items`
- **Topics**: `orders.orders`, `orders.order_items`

### Configuration Features
- **CDC Mode**: Real-time change capture
- **Snapshot**: Initial snapshot on startup
- **Transform**: Routes topics to `orders.*` format
- **Schema**: JSON format without schema registry
- **Performance**: Optimized for high throughput

## 🎛️ Management

### Check Connector Status
```bash
curl http://localhost:8083/connectors/trackops-orders-connector/status
```

### List All Connectors
```bash
curl http://localhost:8083/connectors
```

### Delete Connector
```bash
curl -X DELETE http://localhost:8083/connectors/trackops-orders-connector
```

### View Connector Configuration
```bash
curl http://localhost:8083/connectors/trackops-orders-connector/config
```

## 🔍 Monitoring

### Debezium UI
- URL: http://localhost:8084
- View connector status and metrics
- Monitor topic messages

### Kafka UI
- URL: http://localhost:8080
- View generated topics and messages
- Monitor consumer lag

## 🐛 Troubleshooting

### Common Issues

1. **Connector fails to start**
   - Check PostgreSQL is running and accessible
   - Verify database user has replication permissions
   - Check connector logs: `docker logs trackops-debezium-connect`

2. **No messages in topics**
   - Verify connector is running: `curl http://localhost:8083/connectors/trackops-orders-connector/status`
   - Check if there are database changes
   - Verify topic names in Kafka UI

3. **Permission denied errors**
   - Ensure PostgreSQL user has `REPLICATION` privilege
   - Check database connection settings

### Logs
```bash
# Debezium Connect logs
docker logs trackops-debezium-connect

# PostgreSQL logs
docker logs trackops-postgres-server
```

## 🔄 Switching Strategies

### Use Outbox Pattern (Default)
```properties
app.event-publishing.strategy=outbox
```

### Use Debezium CDC
```properties
app.event-publishing.strategy=debezium
```

## 📊 Event Flow

### With Debezium
```
Database Change → Debezium → Kafka Topic → Consumer Service
```

### With Outbox Pattern
```
Application → Outbox Table → Event Relay → Kafka Topic → Consumer Service
```
