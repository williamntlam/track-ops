# TrackOps - Real-Time Order Tracking System

A production-ready, event-driven microservices architecture for order tracking and inventory management, inspired by systems like Uber Eats and Amazon Logistics. Built with Spring Boot, Kafka, Redis, and PostgreSQL using modern architectural patterns.

## 🎯 System Overview

TrackOps implements a **distributed order management system** with:
- **Event-Driven Architecture** with Kafka for reliable messaging
- **SAGA Pattern** for distributed transaction management
- **Outbox Pattern** for reliable event publishing
- **Domain-Driven Design** (DDD) with clear service boundaries
- **Microservices** with independent databases and deployment

## 🏗️ Architecture

### High-Level System Design

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              TrackOps Microservices                            │
├─────────────────┬─────────────────┬─────────────────┬─────────────────────────┤
│   Order Service │ Inventory Svc   │ Event Relay Svc │     Management Tools    │
│   (Port 8081)   │   (Port 8082)   │   (Port 8083)   │                         │
├─────────────────┼─────────────────┼─────────────────┼─────────────────────────┤
│ • Order Mgmt    │ • Stock Mgmt    │ • Event Pub     │ • Kafka UI (8080)       │
│ • SAGA Pattern  │ • Reservations  │ • Outbox Poll   │ • pgAdmin (5050)        │
│ • Event Sourcing│ • Business Rules│ • Retry Logic   │ • Health Checks         │
│ • Order Items   │ • Low Stock     │ • Error Handle  │ • Monitoring            │
└─────────────────┴─────────────────┴─────────────────┴─────────────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌────────▼────────┐    ┌────────▼────────┐    ┌────────▼────────┐
│ PostgreSQL      │    │ Redis           │    │ Kafka + ZK      │
│ (3 Databases)   │    │ (Caching)       │    │ (Messaging)     │
│ • trackops_orders│   │ • Session Store │    │ • Event Topics  │
│ • trackops_inv  │    │ • Cache Layer   │    │ • Partitions    │
│ • trackops_evt  │    │ • Rate Limiting │    │ • Replication   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Event Flow Architecture

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Client    │    │   Order     │    │ Inventory   │
│  Request    │───▶│  Service    │───▶│  Service    │
└─────────────┘    └─────────────┘    └─────────────┘
                           │                   │
                           ▼                   ▼
                   ┌─────────────┐    ┌─────────────┐
                   │   Outbox    │    │   Kafka     │
                   │   Events    │    │  Messages   │
                   └─────────────┘    └─────────────┘
                           │                   │
                           ▼                   ▼
                   ┌─────────────┐    ┌─────────────┐
                   │   Event     │    │  Inventory  │
                   │   Relay     │    │  Events     │
                   │  Service    │    │ (Reserved/  │
                   └─────────────┘    │  Failed)    │
                           │          └─────────────┘
                           ▼
                   ┌─────────────┐
                   │   Kafka     │
                   │  Topics     │
                   └─────────────┘
```

### Database Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Database Design                         │
├─────────────────┬─────────────────┬─────────────────────────────┤
│  Order Service  │ Inventory Svc   │    Event Relay Service      │
│  (trackops_orders)│ (trackops_inv) │    (trackops_event_relay)  │
├─────────────────┼─────────────────┼─────────────────────────────┤
│ • orders        │ • inventory_items│ • outbox_events            │
│ • order_items   │ • inventory_    │                             │
│ • outbox_events │   reservations  │                             │
│ • processed_    │                 │                             │
│   events        │                 │                             │
│ • saga_instances│                 │                             │
│ • saga_steps    │                 │                             │
└─────────────────┴─────────────────┴─────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- **Java 21+** (LTS version)
- **Docker & Docker Compose** (for containerized infrastructure)
- **Gradle 8+** (for building Spring Boot applications)

### One-Command Setup

```bash
# Clone and start everything
git clone <repository-url>
cd track-ops
./start-infrastructure.sh
```

This single command will:
- Start all PostgreSQL databases (3 separate instances)
- Initialize Redis for caching
- Launch Kafka with Zookeeper
- Set up Kafka UI and pgAdmin
- Create all database tables with sample data
- Verify all services are healthy

### Start Microservices

```bash
# Terminal 1: Order Service
cd server && ./gradlew bootRun --args='--spring.profiles.active=docker'

# Terminal 2: Inventory Service  
cd inventory-service && ./gradlew bootRun --args='--spring.profiles.active=docker'

# Terminal 3: Event Relay Service
cd event-relay-service && ./gradlew bootRun --args='--spring.profiles.active=docker'
```

### Access Points

| Service | URL | Description |
|---------|-----|-------------|
| **Order Service** | http://localhost:8081 | Order management API |
| **Inventory Service** | http://localhost:8082 | Inventory management API |
| **Event Relay Service** | http://localhost:8083 | Event processing API |
| **Kafka UI** | http://localhost:8080 | Kafka monitoring dashboard |
| **pgAdmin** | http://localhost:5050 | Database administration |

## 📚 Documentation

All documentation is organized in the [`docs/`](./docs/) directory:

- **[Main Documentation](./docs/README.md)** - Complete project overview
- **[Server Documentation](./docs/server/README.md)** - Order service details
- **[Inventory Service](./docs/inventory-service/README.md)** - Inventory management
- **[Event Relay Service](./docs/event-relay-service/README.md)** - Event publishing
- **[Docker Setup](./docs/docker/README.md)** - Container configuration
- **[Docker Setup Guide](./docs/docker/DOCKER-SETUP.md)** - Complete setup instructions
- **[Architecture](./docs/architecture/)** - System design patterns
- **[API Reference](./docs/api/)** - API documentation
- **[Learning Resources](./docs/learning/)** - Tutorials and guides

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Server        │    │  Inventory      │    │  Event Relay    │
│   (Orders)      │    │  Service        │    │  Service        │
│   :8081         │    │  :8082          │    │  :8083          │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌────────▼────────┐    ┌────────▼────────┐    ┌────────▼────────┐
│ PostgreSQL      │    │ Redis           │    │ Kafka           │
│ (3 databases)   │    │ (caching)       │    │ (messaging)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🔄 Event Flow & Patterns

### Order Processing Flow

1. **Order Creation**
   ```
   Client → Order Service → Outbox Event → Event Relay → Kafka
   ```

2. **Inventory Reservation**
   ```
   Kafka → Inventory Service → Inventory Check → Reservation/Rejection
   ```

3. **SAGA Pattern Implementation**
   ```
   Order Created → Inventory Reserved → Payment Processed → Order Confirmed
   ```

### Key Design Patterns

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **Event Sourcing** | Outbox Events | Reliable event publishing |
| **SAGA Pattern** | Distributed transactions | Cross-service consistency |
| **Outbox Pattern** | Database + Kafka | At-least-once delivery |
| **Idempotency** | Event deduplication | Safe retries |

### Event Topics

| Topic | Publisher | Consumer | Description |
|-------|-----------|----------|-------------|
| `order.created` | Order Service | Inventory Service | New order created |
| `order.status.updated` | Order Service | All Services | Order status changed |
| `inventory.reserved` | Inventory Service | Order Service | Inventory successfully reserved |
| `inventory.reservation.failed` | Inventory Service | Order Service | Inventory reservation failed |
| `inventory.released` | Inventory Service | Order Service | Inventory released |

## 🛠️ Technology Stack

### Core Technologies
- **Backend**: Spring Boot 3.2.0, Java 21
- **Database**: PostgreSQL 15 (3 separate instances)
- **Cache**: Redis 7 (session store, caching layer)
- **Messaging**: Apache Kafka 3.6 with Zookeeper
- **Containerization**: Docker, Docker Compose

### Development Tools
- **Build Tool**: Gradle 8+
- **API Documentation**: OpenAPI 3.0 (Swagger)
- **Database Migration**: Flyway
- **Monitoring**: Spring Boot Actuator, Micrometer
- **Testing**: JUnit 5, TestContainers

### Infrastructure
- **Message Broker**: Apache Kafka (3 partitions, replication factor 1)
- **Database**: PostgreSQL 15 (separate databases per service)
- **Cache**: Redis 7 (multiple databases)
- **Management**: Kafka UI, pgAdmin
- **Health Checks**: Built-in Spring Boot Actuator endpoints

## 🏛️ System Design Principles

### Microservices Architecture
- **Service Independence**: Each service has its own database
- **API-First Design**: RESTful APIs with OpenAPI documentation
- **Event-Driven Communication**: Asynchronous messaging via Kafka
- **Fault Isolation**: Service failures don't cascade

### Data Consistency
- **Eventual Consistency**: Through event-driven architecture
- **SAGA Pattern**: For distributed transactions
- **Idempotency**: Safe retry mechanisms
- **Outbox Pattern**: Reliable event publishing

### Scalability & Performance
- **Horizontal Scaling**: Stateless services
- **Caching Strategy**: Redis for frequently accessed data
- **Database Optimization**: Proper indexing and query optimization
- **Message Partitioning**: Kafka topics with multiple partitions

### Reliability & Resilience
- **Retry Logic**: Exponential backoff for transient failures
- **Health Checks**: Comprehensive monitoring
- **Graceful Degradation**: System continues with reduced functionality

## 🔌 API Endpoints

### Order Service (Port 8081)
```
POST   /api/orders              # Create new order
GET    /api/orders              # List orders (paginated)
GET    /api/orders/{id}         # Get order details
PUT    /api/orders/{id}/status  # Update order status
DELETE /api/orders/{id}         # Cancel order
GET    /api/orders/{id}/items   # Get order items
GET    /actuator/health         # Health check
```

### Inventory Service (Port 8082)
```
POST   /api/inventory/items           # Create inventory item
GET    /api/inventory/items           # List inventory items
GET    /api/inventory/items/{id}      # Get item details
PUT    /api/inventory/items/{id}      # Update item
DELETE /api/inventory/items/{id}      # Delete item
POST   /api/inventory/reservations    # Create reservation
GET    /api/inventory/reservations    # List reservations
GET    /api/inventory/health          # Inventory health summary
GET    /actuator/health               # Health check
```

### Event Relay Service (Port 8083)
```
GET    /api/events/unprocessed        # Get unprocessed events
POST   /api/events/process            # Process specific event
POST   /api/events/process-all        # Process all unprocessed events
GET    /api/events/stats              # Event processing statistics
GET    /actuator/health               # Health check
```

## 📊 Monitoring & Observability

### Health Checks
- **Application Health**: `/actuator/health` on each service
- **Database Health**: Connection status and query performance
- **Kafka Health**: Topic and partition status
- **Redis Health**: Cache connectivity and memory usage

### Metrics & Monitoring
- **Application Metrics**: Spring Boot Actuator + Micrometer
- **Database Metrics**: Connection pools, query performance
- **Kafka Metrics**: Message throughput, lag, partition health
- **System Metrics**: CPU, memory, disk usage

### Logging
- **Structured Logging**: JSON format for easy parsing
- **Correlation IDs**: Track requests across services
- **Error Tracking**: Comprehensive error logging and alerting
- **Audit Trail**: Track all business operations

## 🚀 Deployment & Operations

### Docker Deployment
```bash
# Start all infrastructure
./start-infrastructure.sh

# Start microservices
docker-compose up -d server inventory-service event-relay-service
```

### Environment Configuration
- **Local Development**: `application.properties`
- **Docker Environment**: `application-docker.properties`
- **Production**: Environment variables and external config

### Database Management
- **Migrations**: Flyway for schema versioning
- **Backups**: Automated database backups
- **Monitoring**: pgAdmin for database administration
- **Performance**: Query optimization and indexing

### Scaling Considerations
- **Horizontal Scaling**: Multiple instances per service
- **Database Scaling**: Read replicas for read-heavy workloads
- **Cache Scaling**: Redis cluster for high availability
- **Message Scaling**: Kafka partitioning and consumer groups

## 🧪 Testing Strategy

### Test Types
- **Unit Tests**: Individual component testing
- **Integration Tests**: Service-to-service communication
- **Contract Tests**: API contract validation
- **End-to-End Tests**: Complete user journey testing

### Test Data
- **Sample Data**: Pre-loaded test datasets
- **Test Containers**: Isolated test environments
- **Mock Services**: Simulated external dependencies
- **Data Cleanup**: Automated test data cleanup

## 🔒 Security Considerations

### API Security
- **Authentication**: JWT tokens (planned)
- **Authorization**: Role-based access control
- **Rate Limiting**: Prevent API abuse
- **Input Validation**: Comprehensive request validation

### Data Security
- **Encryption**: Data encryption at rest and in transit
- **Secrets Management**: Secure credential storage
- **Audit Logging**: Track all data access
- **Compliance**: GDPR and data privacy compliance

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
