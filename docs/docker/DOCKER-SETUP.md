# 🐳 TrackOps Docker Setup Guide

Complete Docker containerization for all TrackOps microservices with databases and message brokers.

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Server        │    │  Inventory      │    │  Event Relay    │
│   (Orders)      │    │  Service        │    │  Service        │
│   Port: 8081    │    │  Port: 8082     │    │  Port: 8083     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌────────▼────────┐    ┌────────▼────────┐    ┌────────▼────────┐
│ PostgreSQL      │    │ PostgreSQL      │    │ PostgreSQL      │
│ trackops_orders │    │ trackops_       │    │ trackops_event_ │
│ Port: 5432      │    │ inventory       │    │ relay           │
│                 │    │ Port: 5433      │    │ Port: 5434      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌────────▼────────┐    ┌────────▼────────┐    ┌────────▼────────┐
│ Redis           │    │ Kafka           │    │ pgAdmin         │
│ Port: 6379      │    │ Port: 9092      │    │ Port: 5050      │
│ DB: 0,1,2       │    │ UI: 8080        │    │ admin@trackops  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🚀 Quick Start

### 1. Start Infrastructure
```bash
# Start all databases and message brokers (includes database initialization)
./scripts/start-infrastructure.sh

# Or manually
docker compose up -d postgres-server postgres-inventory postgres-event-relay redis kafka
./scripts/init-databases.sh
```

### 2. Start Microservices
```bash
# Server (Order Service)
cd server
./gradlew bootRun --args='--spring.profiles.active=docker'

# Inventory Service  
cd inventory-service
./gradlew bootRun --args='--spring.profiles.active=docker'

# Event Relay Service
cd event-relay-service
./gradlew bootRun --args='--spring.profiles.active=docker'
```

## 📊 Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| **Server** | http://localhost:8081 | Order management API |
| **Inventory** | http://localhost:8082 | Inventory management API |
| **Event Relay** | http://localhost:8083 | Event processing API |
| **Kafka UI** | http://localhost:8080 | Kafka monitoring |
| **pgAdmin** | http://localhost:5050 | Database management |

## 🗄️ Database Connections

| Service | Database | Host | Port | Username | Password |
|---------|----------|------|------|----------|----------|
| **Server** | trackops_orders | localhost | 5432 | postgres | password |
| **Inventory** | trackops_inventory | localhost | 5433 | postgres | password |
| **Event Relay** | trackops_event_relay | localhost | 5434 | postgres | password |
| **Redis** | - | localhost | 6379 | - | - |

## 📊 Database Tables

### Order Service (trackops_orders)
- **orders** - Order information and status
- **order_items** - Individual items within orders
- **outbox_events** - Event sourcing for reliable messaging
- **processed_events** - Idempotency tracking
- **saga_instances** - SAGA pattern for distributed transactions
- **saga_steps** - Individual SAGA steps

### Inventory Service (trackops_inventory)
- **inventory_items** - Product catalog with stock levels
- **inventory_reservations** - Reserved inventory for orders

### Event Relay Service (trackops_event_relay)
- **outbox_events** - Events to be published to Kafka

## 🔧 Configuration Files

### Docker Profiles
- `server/src/main/resources/application-docker.properties`
- `inventory-service/src/main/resources/application-docker.properties`
- `event-relay-service/src/main/resources/application-docker.properties`

### Database Migrations
- `server/src/main/resources/db/migration/V1__Create_orders_tables.sql`
- `server/src/main/resources/db/migration/V2__Create_processed_events_table.sql`
- `server/src/main/resources/db/migration/V3__Create_saga_tables.sql`
- `inventory-service/src/main/resources/db/migration/V1__Create_inventory_tables.sql`
- `inventory-service/src/main/resources/db/migration/V2__Add_inventory_business_fields.sql`
- `event-relay-service/src/main/resources/db/migration/V1__Create_outbox_events_table.sql`

### Database Initialization Scripts
- `scripts/init-databases.sh` - Comprehensive database setup with tables and sample data
- `server/setup-database.sql` - Order service specific setup
- `inventory-service/setup-database.sql` - Inventory service specific setup
- `event-relay-service/setup-database.sql` - Event relay service specific setup
- `scripts/setup-all-databases.sql` - Complete database setup for all services

## 🐳 Docker Compose Services

### Databases
- **postgres-server**: Order service database
- **postgres-inventory**: Inventory service database  
- **postgres-event-relay**: Event relay service database

### Message Brokers
- **redis**: Caching layer (databases 0, 1, 2)
- **kafka**: Event streaming platform (KRaft mode - no ZooKeeper required)

### Management Tools
- **kafka-ui**: Kafka monitoring interface
- **pgadmin**: Database administration

## 🔍 Health Checks

### Application Health
```bash
# Server
curl http://localhost:8081/actuator/health

# Inventory Service
curl http://localhost:8082/actuator/health

# Event Relay Service
curl http://localhost:8083/actuator/health
```

### Database Health
```bash
# Check PostgreSQL services
docker-compose exec postgres-server pg_isready -U postgres -d trackops_orders
docker-compose exec postgres-inventory pg_isready -U postgres -d trackops_inventory
docker-compose exec postgres-event-relay pg_isready -U postgres -d trackops_event_relay

# Check Redis
docker-compose exec redis redis-cli ping

# Check Kafka
docker-compose exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

## 🛠️ Troubleshooting

### Common Issues

1. **Port Conflicts**
   - Ensure ports 5432-5434, 6379, 8080-8083, 9092, 5050 are available

2. **Database Connection Issues**
   - Wait for databases to be fully initialized (30-60 seconds)
   - Check database health with provided commands

3. **Kafka Connection Issues**
   - Kafka uses KRaft mode (no ZooKeeper required)
   - Check Kafka UI at http://localhost:8080
   - Verify Kafka is healthy: `docker-compose exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092`

4. **Service Startup Issues**
   - Check logs: `docker-compose logs [service-name]`
   - Verify environment variables in docker-compose.yml

### Logs
```bash
# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f postgres-server
docker-compose logs -f kafka
docker-compose logs -f redis
```

## 🧹 Cleanup

### Stop All Services
```bash
docker-compose down
```

### Remove All Data (⚠️ Destructive)
```bash
docker-compose down -v
docker system prune -a
```

## 📈 Monitoring

### pgAdmin Access
- URL: http://localhost:5050
- Email: admin@trackops.com
- Password: admin

### Kafka UI Access
- URL: http://localhost:8080
- Cluster: local
- Bootstrap Servers: kafka:29092

### Application Metrics
- Server: http://localhost:8081/actuator/prometheus
- Inventory: http://localhost:8082/actuator/prometheus
- Event Relay: http://localhost:8083/actuator/prometheus
