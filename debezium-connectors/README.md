# Debezium Connector Configuration

This directory contains Debezium connector configurations for TrackOps.

## 📁 Files

- `trackops-orders-connector.json` - PostgreSQL connector for orders database
- `trackops-inventory-reserve-outbox-connector.json` - PostgreSQL connector for inventory reserve outbox table (used when `app.inventory.reserve-request.mode=cdc`)
- `setup-debezium-connectors.sh` - Script to create connectors
- `README.md` - This documentation

## 🚀 Quick Start

### 1. Start Infrastructure with Debezium
```bash
# Start all services including Debezium
./scripts/start-infrastructure.sh
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

### TrackOps Inventory Reserve Outbox Connector (CDC mode)
- **Name**: `trackops-inventory-reserve-outbox-connector`
- **Table**: `inventory_reserve_outbox`
- **Topic**: `trackops_orders.public.inventory_reserve_outbox`
- **Purpose**: When `app.inventory.reserve-request.mode=cdc`, the app enqueues rows to this table in the same transaction as order processing; Debezium streams inserts to Kafka; a relay consumer produces to `INVENTORY_RESERVE_REQUEST` and marks the row SENT.
- **Requirement**: The PostgreSQL publication `debezium_publication` must include `inventory_reserve_outbox` (migration V10 or `ALTER PUBLICATION debezium_publication ADD TABLE inventory_reserve_outbox;`).

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

### With Debezium (Fixed Implementation)
```
Database Change → Debezium → Raw CDC Topics → DebeziumOrderEventConsumer → Application Topics → Inventory Service
```

**Detailed Flow:**
1. **Database Change**: Order created/updated in `trackops_orders` database
2. **Debezium Capture**: Connector captures change and publishes to `trackops_orders.public.orders`
3. **Event Transformation**: `DebeziumOrderEventConsumer` processes CDC events and transforms them:
   - Order creation → `ORDER_CREATED` event
   - Order cancellation → `ORDER_CANCELLED` event
4. **Application Events**: Events published to application topics (`ORDER_CREATED`, `ORDER_CANCELLED`)
5. **Inventory Service**: Consumes application events for inventory management

### With Outbox Pattern
```
Application → Outbox Table → Event Relay → Kafka Topic → Consumer Service
```

## 🔧 Event Transformation Details

The `DebeziumOrderEventConsumer` bridges Debezium CDC events to application events:

- **Source Topics**: `trackops_orders.public.orders` (raw Debezium CDC events)
- **Target Topics**: `ORDER_CREATED`, `ORDER_CANCELLED` (application events)
- **Event Mapping**:
  - Database INSERT → `ORDER_CREATED` event
  - Database UPDATE (status=CANCELLED) → `ORDER_CANCELLED` event
  - Database DELETE → `ORDER_CANCELLED` event (treated as cancellation)

## 🧪 Testing the Flow

Use the provided test scripts to verify the complete flow:

```bash
# Test basic Debezium flow
./scripts/test-debezium-flow.sh

# Test Redis cache integration
./scripts/test-redis-cache-integration.sh
```

These scripts will:
1. Check infrastructure services are running
2. Verify Debezium connector status
3. Check Kafka topics have messages
4. Test Redis cache invalidation and warming
5. Provide manual testing commands

## 🔄 Redis Cache Integration

### Cache Invalidation Strategy

The Debezium Redis Cache Integration provides real-time cache invalidation:

- **DebeziumRedisCacheConsumer**: Listens to Debezium CDC events and invalidates related cache entries
- **DebeziumRedisCacheWarmer**: Proactively updates cache with fresh data from database changes

### Cache Patterns Invalidated

When database changes occur, the following cache patterns are invalidated:

- `order:entity:{orderId}` - Full order objects
- `order:response:{orderId}` - API response objects  
- `order:status:{orderId}` - Order status cache
- `orders:status:{status}` - Orders by status
- `orders:customer:{customerId}` - Orders by customer
- `orders:page:*` - Paginated results

### Cache Warming Strategy

Cache warming proactively updates cache entries with fresh data:

- **Order Status**: Automatically updated when order status changes
- **Order Entities**: Can be warmed for frequently accessed orders
- **TTL Management**: Configurable TTL for different cache types

### Configuration

```properties
# Cache TTL Configuration
app.cache.ttl.order=3600          # Order entities: 1 hour
app.cache.ttl.status=1800        # Order status: 30 minutes
app.cache.ttl.response=1800       # API responses: 30 minutes
app.cache.ttl.customer=3600      # Customer orders: 1 hour
app.cache.ttl.page=900           # Page results: 15 minutes

# Cache Features
app.cache.warming.enabled=true    # Enable cache warming
app.cache.invalidation.enabled=true # Enable cache invalidation
```
