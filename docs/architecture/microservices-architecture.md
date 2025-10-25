# Microservices Architecture - TrackOps

## 🏗️ Architecture Overview

TrackOps now implements a true **microservices architecture** with three distinct services:

1. **Order Service** - Core business logic and order management
2. **Inventory Service** - Inventory reservations and releases
3. **Event Relay Service** - Change Data Capture (CDC) and event publishing

## 📊 Service Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        TrackOps Microservices                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐    ┌──────────────┐ │
│  │   Order Service │    │ Inventory Service│    │Event Relay   │ │
│  │                 │    │                 │    │   Service    │ │
│  │ • Order CRUD    │    │ • Inventory Mgmt│    │ • CDC        │ │
│  │ • Business Logic│    │ • Reservations  │    │ • Event Pub  │ │
│  │ • Outbox Events │    │ • Event Response│    │ • Retry Logic│ │
│  └─────────────────┘    └─────────────────┘    └──────────────┘ │
│           │                       │                       │     │
│           │                       │                       │     │
│           ▼                       ▼                       ▼     │
│  ┌─────────────────┐    ┌─────────────────┐    ┌──────────────┐ │
│  │   PostgreSQL    │    │   PostgreSQL    │    │   PostgreSQL │ │
│  │  (trackops)     │    │(trackops_inv)   │    │  (trackops)  │ │
│  └─────────────────┘    └─────────────────┘    └──────────────┘ │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    Apache Kafka                            │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │ │
│  │  │ORDER_CREATED│ │ORDER_STATUS │ │INVENTORY_*  │          │ │
│  │  │             │ │_UPDATED     │ │             │          │ │
│  │  └─────────────┘ └─────────────┘ └─────────────┘          │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                      Redis Cache                           │ │
│  │  • Order Status Cache  • Session Management               │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## 🔄 Event Flow Architecture

### 1. Order Creation Flow
```
Order Service → Creates Order → Saves to DB → Creates Outbox Event
     ↓
Event Relay Service → Polls Outbox → Publishes ORDER_CREATED → Kafka
     ↓
Inventory Service → Consumes ORDER_CREATED → Reserves Inventory
     ↓
Inventory Service → Publishes INVENTORY_RESERVED → Kafka
     ↓
Order Service → Consumes INVENTORY_RESERVED → Updates Order Status
```

### 2. Order Cancellation Flow
```
Order Service → Cancels Order → Saves to DB → Creates Outbox Event
     ↓
Event Relay Service → Polls Outbox → Publishes ORDER_CANCELLED → Kafka
     ↓
Inventory Service → Consumes ORDER_CANCELLED → Releases Inventory
     ↓
Inventory Service → Publishes INVENTORY_RELEASED → Kafka
     ↓
Order Service → Consumes INVENTORY_RELEASED → Logs Release
```

## 🏢 Service Responsibilities

### Order Service
- **Primary Database**: `trackops` (PostgreSQL)
- **Responsibilities**:
  - Order CRUD operations
  - Business logic and validation
  - Order status management
  - Outbox event creation
  - Event consumption (inventory responses)
- **Ports**: 8080
- **Dependencies**: PostgreSQL, Redis, Kafka (consumer)

### Inventory Service
- **Primary Database**: `trackops_inventory` (PostgreSQL)
- **Responsibilities**:
  - Inventory item management
  - Reservation and release logic
  - Event consumption (order events)
  - Event publishing (inventory responses)
- **Ports**: 8081
- **Dependencies**: PostgreSQL, Redis, Kafka (producer/consumer)

### Event Relay Service
- **Primary Database**: `trackops` (PostgreSQL) - Read-only
- **Responsibilities**:
  - Outbox event polling
  - Kafka event publishing
  - Retry logic and error handling
  - Event cleanup
- **Ports**: 8082
- **Dependencies**: PostgreSQL (Order Service DB), Kafka (producer)

## 📡 Event Topics

### Order Events (Published by Event Relay Service)
- `ORDER_CREATED` - New order created
- `ORDER_STATUS_UPDATED` - Order status changed
- `ORDER_DELIVERED` - Order delivered
- `ORDER_CANCELLED` - Order cancelled

### Inventory Events (Published by Inventory Service)
- `INVENTORY_RESERVED` - Inventory successfully reserved
- `INVENTORY_RESERVATION_FAILED` - Inventory reservation failed
- `INVENTORY_RELEASED` - Inventory released

## 🗄️ Database Schema

### Order Service Database (`trackops`)
```sql
-- Core tables
orders
order_items
customers
addresses

-- Event sourcing
outbox_events
processed_events

-- SAGA management
saga_instances
saga_steps
```

### Inventory Service Database (`trackops_inventory`)
```sql
-- Core tables
inventory_items
inventory_reservations

-- Event tracking
processed_events
```

## 🔧 Configuration

### Service Ports
- **Order Service**: 8080
- **Inventory Service**: 8081
- **Event Relay Service**: 8082

### Database Connections
- **Order Service**: `jdbc:postgresql://localhost:5432/trackops`
- **Inventory Service**: `jdbc:postgresql://localhost:5432/trackops_inventory`
- **Event Relay Service**: `jdbc:postgresql://localhost:5432/trackops` (read-only)

### Kafka Configuration
- **Bootstrap Servers**: `localhost:9092`
- **Consumer Groups**:
  - Order Service: `trackops-orders`
  - Inventory Service: `inventory-service`
- **Producer**: Event Relay Service

## 🚀 Deployment

### Docker Compose
```bash
# Start all services
docker-compose up -d

# Start specific services
docker-compose up order-service inventory-service event-relay-service
```

### Kubernetes
```bash
# Deploy all services
kubectl apply -f k8s/

# Deploy specific service
kubectl apply -f k8s/order-service.yaml
kubectl apply -f k8s/inventory-service.yaml
kubectl apply -f k8s/event-relay-service.yaml
```

## 📈 Monitoring

### Health Checks
- **Order Service**: `http://localhost:8080/actuator/health`
- **Inventory Service**: `http://localhost:8081/actuator/health`
- **Event Relay Service**: `http://localhost:8082/actuator/health`

### Metrics
- **Order Service**: Order processing metrics, cache hit rates
- **Inventory Service**: Inventory levels, reservation success rates
- **Event Relay Service**: Event processing rates, backlog sizes

### Prometheus Integration
All services expose metrics on `/actuator/prometheus`

## 🔍 Troubleshooting

### Common Issues

1. **Event Relay Service Not Processing Events**
   - Check database connection to Order Service
   - Verify outbox_events table has unprocessed events
   - Check Kafka connectivity

2. **Inventory Service Not Responding**
   - Check inventory database connection
   - Verify Kafka consumer group configuration
   - Check inventory item data

3. **Order Service Not Receiving Inventory Responses**
   - Check Kafka topic configuration
   - Verify consumer group settings
   - Check event deserialization

### Logs
```bash
# Docker logs
docker logs trackops-order-service
docker logs trackops-inventory-service
docker logs trackops-event-relay-service

# Kubernetes logs
kubectl logs -l app=order-service
kubectl logs -l app=inventory-service
kubectl logs -l app=event-relay-service
```

## 🎯 Benefits of This Architecture

### 1. **Separation of Concerns**
- Each service has a single responsibility
- Clear boundaries between business domains
- Independent development and deployment

### 2. **Scalability**
- Services can be scaled independently
- Database connections are isolated
- Event processing can be distributed

### 3. **Reliability**
- Event-driven communication ensures loose coupling
- Outbox pattern guarantees event delivery
- Retry mechanisms handle transient failures

### 4. **Maintainability**
- Clear service boundaries
- Independent testing and deployment
- Technology stack flexibility per service

### 5. **Observability**
- Service-specific health checks
- Detailed metrics and monitoring
- Centralized logging and tracing

## 🔄 Migration from Monolith

### Phase 1: Extract Event Relay Service
- Move outbox publishing logic to separate service
- Maintain existing Order Service functionality
- Add event relay service

### Phase 2: Extract Inventory Service
- Create inventory service with event handling
- Update Order Service to handle inventory responses
- Implement inventory-specific database

### Phase 3: Optimize and Scale
- Fine-tune event processing
- Add monitoring and alerting
- Implement advanced retry strategies
