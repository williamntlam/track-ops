# TrackOps - Enterprise-Grade Real-Time Order Tracking System

A production-ready, event-driven microservices architecture implementing **Change Data Capture (CDC)**, **SAGA patterns**, and **distributed caching** for enterprise-scale order tracking and inventory management. Built with Spring Boot, Apache Kafka, Debezium CDC, Redis, and PostgreSQL using advanced architectural patterns.

## 🎯 System Overview

TrackOps implements a **sophisticated distributed order management system** featuring:

- **🔄 Change Data Capture (CDC)** with Debezium for real-time database synchronization
- **📡 Event-Driven Architecture** with Kafka for reliable, scalable messaging
- **🔄 SAGA Pattern** for distributed transaction management and consistency
- **📦 Outbox Pattern** with dual strategies (Outbox + Debezium CDC)
- **⚡ Redis Caching** with intelligent invalidation and warming strategies
- **🏗️ Domain-Driven Design (DDD)** with clear service boundaries
- **🔧 Microservices** with independent databases and deployment
- **🛡️ Dead Letter Queues (DLQ)** for fault tolerance and error handling
- **📊 Comprehensive Monitoring** with health checks and metrics

## 🏗️ Advanced Architecture

### High-Level System Design

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              TrackOps Enterprise Microservices                          │
├─────────────────┬─────────────────┬─────────────────┬─────────────────┬─────────────────┤
│   Order Service │ Inventory Svc   │ Event Relay Svc │ Debezium Connect│ Management Tools│
│   (Port 8081)   │   (Port 8082)   │   (Port 8084)   │   (Port 8083)   │                 │
├─────────────────┼─────────────────┼─────────────────┼─────────────────┼─────────────────┤
│ • Order Mgmt    │ • Stock Mgmt    │ • Event Pub     │ • CDC Bridge    │ • Kafka UI      │
│ • SAGA Pattern  │ • Reservations  │ • Outbox Poll   │ • Event Transform│ • Debezium UI   │
│ • Event Sourcing│ • Business Rules│ • Retry Logic   │ • Cache Inval   │ • pgAdmin       │
│ • Redis Cache   │ • Low Stock     │ • Error Handle  │ • Cache Warming │ • Health Checks │
│ • DLQ Handling  │ • Redis Cache   │ • DLQ Monitor   │ • DLQ Recovery  │ • Monitoring    │
└─────────────────┴─────────────────┴─────────────────┴─────────────────┴─────────────────┘
         │                       │                       │                       │
         └───────────────────────┼───────────────────────┼───────────────────────┘
                                 │                       │
         ┌───────────────────────┼───────────────────────┼───────────────────────┐
         │                       │                       │                       │
┌────────▼────────┐    ┌────────▼────────┐    ┌────────▼────────┐    ┌────────▼────────┐
│ PostgreSQL      │    │ Redis           │    │ Kafka + ZK      │    │ Debezium        │
│ (3 Databases)   │    │ (Multi-DB Cache)│    │ (Messaging)     │    │ CDC Topics      │
│ • trackops_orders│   │ • Session Store │    │ • Event Topics  │    │ • Raw CDC Events│
│ • trackops_inv  │    │ • Cache Layer   │    │ • DLQ Topics    │    │ • Transform     │
│ • trackops_evt  │    │ • Rate Limiting │    │ • Partitions    │    │ • Application   │
│ • CDC Enabled   │    │ • TTL Management│    │ • Replication   │    │   Events        │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Event Flow Architecture with CDC

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Client    │    │   Order     │    │ PostgreSQL  │    │ Debezium    │
│  Request    │───▶│  Service    │───▶│ Database    │───▶│ CDC         │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
                           │                                       │
                           ▼                                       ▼
                   ┌─────────────┐                        ┌─────────────┐
                   │   Redis     │                        │   Kafka     │
                   │   Cache     │                        │ CDC Topics  │
                   └─────────────┘                        └─────────────┘
                           │                                       │
                           ▼                                       ▼
                   ┌─────────────┐                        ┌─────────────┐
                   │   Cache    │                        │ Debezium    │
                   │ Invalidation│                        │ Consumers   │
                   └─────────────┘                        └─────────────┘
                                                                    │
                                                                    ▼
                                                           ┌─────────────┐
                                                           │ Application │
                                                           │   Events    │
                                                           │ (ORDER_CREATED│
                                                           │ ORDER_CANCELLED)│
                                                           └─────────────┘
                                                                    │
                                                                    ▼
                                                           ┌─────────────┐
                                                           │ Inventory   │
                                                           │  Service    │
                                                           └─────────────┘
```

### Database Architecture with CDC

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        Database Design with Change Data Capture                         │
├─────────────────┬─────────────────┬─────────────────┬─────────────────────────────────┤
│  Order Service  │ Inventory Svc   │ Event Relay Svc │        Debezium CDC             │
│  (trackops_orders)│ (trackops_inv) │ (trackops_event_relay)│                             │
├─────────────────┼─────────────────┼─────────────────┼─────────────────────────────────┤
│ • orders        │ • inventory_items│ • outbox_events │ • trackops_orders.public.orders │
│ • order_items   │ • inventory_    │                 │ • trackops_orders.public.order_  │
│ • outbox_events │   reservations  │                 │   items                         │
│ • processed_    │                 │                 │ • Raw CDC events → Application  │
│   events        │                 │                 │   events transformation        │
│ • saga_instances│                 │                 │ • Cache invalidation triggers    │
│ • saga_steps    │                 │                 │ • Cache warming strategies      │
│ • CDC Enabled   │ • CDC Enabled   │                 │ • DLQ error handling           │
│   (Publication) │   (Publication) │                 │                                 │
└─────────────────┴─────────────────┴─────────────────┴─────────────────────────────────┘
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
./scripts/start-all-microservices.sh
```

This single command will:
- Start all PostgreSQL databases (3 separate instances) with CDC enabled
- Initialize Redis for multi-database caching
- Launch Kafka with Zookeeper and DLQ topics
- Set up Debezium Connect for CDC
- Configure Debezium connectors for real-time data capture
- Set up Kafka UI, Debezium UI, and pgAdmin
- Create all database tables with sample data
- Start all microservices with proper configuration
- Verify all services are healthy

### Access Points

| Service | URL | Description |
|---------|-----|-------------|
| **Order Service** | http://localhost:8081 | Order management API with Redis caching |
| **Inventory Service** | http://localhost:8082 | Inventory management API with Redis caching |
| **Event Relay Service** | http://localhost:8084 | Event processing API |
| **Debezium Connect** | http://localhost:8083 | CDC connector management |
| **Kafka UI** | http://localhost:8080 | Kafka monitoring dashboard |
| **Debezium UI** | http://localhost:8084 | Debezium CDC monitoring |
| **pgAdmin** | http://localhost:5050 | Database administration |

## 🔄 Advanced Event Flow & Patterns

### Change Data Capture (CDC) Flow

1. **Database Change Detection**
   ```
   PostgreSQL → Debezium CDC → Raw CDC Events → Event Transformation → Application Events
   ```

2. **Cache Management Flow**
   ```
   Database Change → CDC Event → Cache Invalidation → Cache Warming → Updated Cache
   ```

3. **Dual Event Publishing Strategy**
   ```
   Order Service → [Outbox Pattern + Debezium CDC] → Kafka Topics → Consumer Services
   ```

### SAGA Pattern Implementation

```
Order Created → Inventory Reserved → Payment Processed → Order Confirmed
     ↓              ↓                    ↓                ↓
   CDC Event    CDC Event          CDC Event        CDC Event
     ↓              ↓                    ↓                ↓
 Cache Inval   Cache Inval        Cache Inval      Cache Inval
```

### Key Design Patterns

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **Change Data Capture** | Debezium CDC | Real-time database synchronization |
| **Event Sourcing** | Outbox + CDC Events | Reliable event publishing |
| **SAGA Pattern** | Distributed transactions | Cross-service consistency |
| **Outbox Pattern** | Database + Kafka | At-least-once delivery |
| **Cache-Aside Pattern** | Redis + CDC | High-performance caching |
| **Dead Letter Queue** | Kafka DLQ | Fault tolerance and error handling |
| **Idempotency** | Event deduplication | Safe retries |
| **Circuit Breaker** | Resilience patterns | System stability |

### Event Topics

| Topic | Publisher | Consumer | Description |
|-------|-----------|----------|-------------|
| `trackops_orders.public.orders` | Debezium CDC | Order Event Consumer | Raw CDC events for orders |
| `trackops_orders.public.order_items` | Debezium CDC | Order Event Consumer | Raw CDC events for order items |
| `ORDER_CREATED` | Order Service | Inventory Service | Application event: new order |
| `ORDER_CANCELLED` | Order Service | Inventory Service | Application event: order cancelled |
| `order.created` | Order Service | Inventory Service | Legacy: new order created |
| `order.status.updated` | Order Service | All Services | Order status changed |
| `inventory.reserved` | Inventory Service | Order Service | Inventory successfully reserved |
| `inventory.reservation.failed` | Inventory Service | Order Service | Inventory reservation failed |
| `inventory.released` | Inventory Service | Order Service | Inventory released |
| `*.dlq` | Error Handler | DLQ Monitor | Failed messages for analysis |

## 🛠️ Technology Stack

### Core Technologies
- **Backend**: Spring Boot 3.2.0, Java 21
- **Database**: PostgreSQL 15 (3 separate instances with CDC)
- **Cache**: Redis 7 (multi-database caching with TTL)
- **Messaging**: Apache Kafka 3.6 with Zookeeper
- **CDC**: Debezium 2.4 for Change Data Capture
- **Containerization**: Docker, Docker Compose

### Advanced Features
- **Change Data Capture**: Debezium CDC for real-time data synchronization
- **Distributed Caching**: Redis with intelligent invalidation strategies
- **Dead Letter Queues**: Kafka DLQ for fault tolerance
- **Event Transformation**: CDC events → Application events
- **Cache Warming**: Proactive cache updates from CDC events
- **Health Monitoring**: Comprehensive health checks and metrics

### Development Tools
- **Build Tool**: Gradle 8+
- **API Documentation**: OpenAPI 3.0 (Swagger)
- **Database Migration**: Flyway
- **Monitoring**: Spring Boot Actuator, Micrometer
- **Testing**: JUnit 5, TestContainers
- **CDC Management**: Debezium Connect REST API

### Infrastructure
- **Message Broker**: Apache Kafka (3 partitions, replication factor 1)
- **Database**: PostgreSQL 15 (separate databases per service, CDC enabled)
- **Cache**: Redis 7 (multiple databases, TTL management)
- **CDC Engine**: Debezium Connect with PostgreSQL connector
- **Management**: Kafka UI, Debezium UI, pgAdmin
- **Health Checks**: Built-in Spring Boot Actuator endpoints

## 🏛️ System Design Principles

### Microservices Architecture
- **Service Independence**: Each service has its own database and cache
- **API-First Design**: RESTful APIs with OpenAPI documentation
- **Event-Driven Communication**: Asynchronous messaging via Kafka
- **CDC Integration**: Real-time data synchronization across services
- **Fault Isolation**: Service failures don't cascade

### Data Consistency & Synchronization
- **Change Data Capture**: Real-time database change propagation
- **Eventual Consistency**: Through event-driven architecture
- **SAGA Pattern**: For distributed transactions
- **Cache Consistency**: Intelligent invalidation and warming
- **Idempotency**: Safe retry mechanisms
- **Outbox Pattern**: Reliable event publishing

### Scalability & Performance
- **Horizontal Scaling**: Stateless services with shared cache
- **Intelligent Caching**: Redis with CDC-driven invalidation
- **Database Optimization**: Proper indexing and query optimization
- **Message Partitioning**: Kafka topics with multiple partitions
- **Cache Warming**: Proactive cache updates for performance

### Reliability & Resilience
- **Dead Letter Queues**: Failed message handling and recovery
- **Retry Logic**: Exponential backoff for transient failures
- **Health Checks**: Comprehensive monitoring and alerting
- **Circuit Breakers**: Prevent cascade failures
- **Graceful Degradation**: System continues with reduced functionality

### 📊 Monitoring & Observability

#### Health Checks
- **Application Health**: `/actuator/health` endpoint available on all microservices  
- **Database Health**: Monitors connection status, query latency, and performance  
- **Kafka Health**: Tracks topic, partition, and consumer group status  
- **Redis Health**: Reports cache connectivity, memory usage, and eviction stats  
- **Debezium Health**: Observes CDC connector lag, state, and throughput  
- **Cache Health**: Measures cache hit rate, TTL performance, and invalidation patterns  

#### Metrics & Monitoring
- **Application Metrics**: Collected via Spring Boot Actuator and Micrometer  
- **Database Metrics**: Includes connection pool usage and query response times  
- **Kafka Metrics**: Monitors message throughput, lag, and partition utilization  
- **CDC Metrics**: Tracks Debezium connector lag, event rates, and latency  
- **Cache Metrics**: Reports hit/miss ratios, TTL efficiency, and cache warming stats  
- **System Metrics**: CPU, memory, and disk utilization for all microservices  
- **Prometheus Integration**:  
  - Centralized metrics aggregation across services  
  - Scrapes `/actuator/prometheus` endpoints exposed by each service  
  - Integrates with **Grafana** for real-time dashboards and visualization  
  - Monitors Kafka lag, CDC event latency, cache hit ratio, and service uptime  

#### Advanced Monitoring Features
- **Dead Letter Queue Monitoring**: Detects and analyzes failed message deliveries  
- **Cache Performance**: Evaluates hit/miss ratios and invalidation behavior  
- **CDC Lag Monitoring**: Compares real-time application events vs. CDC propagation delay  
- **Event Processing Metrics**: Captures message throughput, latency, and error rates  
- **SAGA Pattern Monitoring**: Tracks distributed transaction success and rollback rates  

#### Logging
- **Structured Logging**: JSON-formatted logs for easier parsing and analysis  
- **Correlation IDs**: Enables cross-service request tracing and debugging  
- **CDC Event Logging**: Captures end-to-end database change propagation  
- **Cache Operation Logging**: Logs cache hits, misses, and invalidation triggers  
- **Error Tracking**: Comprehensive error logs for alerting and recovery  
- **Audit Trail**: Full business operation tracking for compliance and debugging  


## 🔌 API Endpoints

### Order Service (Port 8081)
```
POST   /api/orders              # Create new order
GET    /api/orders              # List orders (paginated, cached)
GET    /api/orders/{id}         # Get order details (cached)
PUT    /api/orders/{id}/status  # Update order status
DELETE /api/orders/{id}         # Cancel order
GET    /api/orders/{id}/items   # Get order items (cached)
GET    /actuator/health         # Health check (includes Debezium status)
GET    /actuator/cache          # Cache statistics
```

### Inventory Service (Port 8082)
```
POST   /api/inventory/items           # Create inventory item
GET    /api/inventory/items           # List inventory items (cached)
GET    /api/inventory/items/{id}      # Get item details (cached)
PUT    /api/inventory/items/{id}      # Update item
DELETE /api/inventory/items/{id}      # Delete item
POST   /api/inventory/reservations    # Create reservation
GET    /api/inventory/reservations    # List reservations (cached)
GET    /api/inventory/health          # Inventory health summary
GET    /actuator/health               # Health check
GET    /actuator/cache                # Cache statistics
```

### Event Relay Service (Port 8084)
```
GET    /api/events/unprocessed        # Get unprocessed events
POST   /api/events/process            # Process specific event
POST   /api/events/process-all        # Process all unprocessed events
GET    /api/events/stats              # Event processing statistics
GET    /actuator/health               # Health check
```

### Debezium Connect (Port 8083)
```
GET    /connectors                    # List all connectors
POST   /connectors                    # Create new connector
GET    /connectors/{name}             # Get connector details
PUT    /connectors/{name}/config      # Update connector config
POST   /connectors/{name}/restart     # Restart connector
DELETE /connectors/{name}             # Delete connector
GET    /connectors/{name}/status      # Get connector status
```

## 📊 Monitoring & Observability

### Health Checks
- **Application Health**: `/actuator/health` on each service
- **Database Health**: Connection status and query performance
- **Kafka Health**: Topic and partition status
- **Redis Health**: Cache connectivity and memory usage
- **Debezium Health**: CDC connector status and lag
- **Cache Health**: Hit rates, TTL, and invalidation metrics

### Metrics & Monitoring
- **Application Metrics**: Spring Boot Actuator + Micrometer
- **Database Metrics**: Connection pools, query performance
- **Kafka Metrics**: Message throughput, lag, partition health
- **CDC Metrics**: Debezium connector lag and throughput
- **Cache Metrics**: Hit rates, miss rates, TTL effectiveness
- **System Metrics**: CPU, memory, disk usage

### Advanced Monitoring Features
- **Dead Letter Queue Monitoring**: Failed message analysis
- **Cache Performance**: Hit/miss ratios, invalidation patterns
- **CDC Lag Monitoring**: Real-time vs CDC event processing
- **Event Processing Metrics**: Throughput, latency, error rates
- **SAGA Pattern Monitoring**: Transaction success/failure rates

### Logging
- **Structured Logging**: JSON format for easy parsing
- **Correlation IDs**: Track requests across services
- **CDC Event Logging**: Track data change propagation
- **Cache Operation Logging**: Hit/miss patterns and invalidation
- **Error Tracking**: Comprehensive error logging and alerting
- **Audit Trail**: Track all business operations

## 🚀 Deployment & Operations

### Docker Deployment
```bash
# Start all infrastructure (includes Debezium)
./scripts/start-all-microservices.sh

# Or start infrastructure only
./scripts/start-infrastructure.sh

# Stop everything
./scripts/stop-all-microservices.sh
```

### Environment Configuration
- **Local Development**: `application.properties`
- **Docker Environment**: `application-docker.properties`
- **CDC Configuration**: Debezium connector configurations
- **Cache Configuration**: Redis TTL and invalidation settings
- **Production**: Environment variables and external config

### Database Management
- **Migrations**: Flyway for schema versioning
- **CDC Setup**: Debezium publications and replication slots
- **Backups**: Automated database backups
- **Monitoring**: pgAdmin for database administration
- **Performance**: Query optimization and indexing

### CDC Management
- **Connector Management**: Debezium Connect REST API
- **Publication Setup**: PostgreSQL logical replication
- **Slot Management**: Replication slot monitoring
- **Event Monitoring**: CDC event processing metrics

### Scaling Considerations
- **Horizontal Scaling**: Multiple instances per service
- **Database Scaling**: Read replicas for read-heavy workloads
- **Cache Scaling**: Redis cluster for high availability
- **Message Scaling**: Kafka partitioning and consumer groups
- **CDC Scaling**: Multiple Debezium connectors for different databases

## 🧪 Testing Strategy

### Test Types
- **Unit Tests**: Individual component testing
- **Integration Tests**: Service-to-service communication
- **CDC Tests**: Change Data Capture functionality
- **Cache Tests**: Redis caching behavior
- **Contract Tests**: API contract validation
- **End-to-End Tests**: Complete user journey testing
- **DLQ Tests**: Dead Letter Queue functionality

### Test Data
- **Sample Data**: Pre-loaded test datasets
- **Test Containers**: Isolated test environments
- **CDC Test Data**: Database change simulation
- **Cache Test Data**: Cache invalidation scenarios
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
- **CDC Security**: Secure database replication
- **Cache Security**: Redis authentication and encryption
- **Secrets Management**: Secure credential storage
- **Audit Logging**: Track all data access
- **Compliance**: GDPR and data privacy compliance

## 📚 Documentation

All documentation is organized in the [`docs/`](./docs/) directory:

- **[Main Documentation](./docs/README.md)** - Complete project overview
- **[Event Publishing Strategy](./docs/EVENT_PUBLISHING_STRATEGY.md)** - CDC and Outbox patterns
- **[Server Documentation](./docs/server/README.md)** - Order service details
- **[Inventory Service](./docs/inventory-service/README.md)** - Inventory management
- **[Event Relay Service](./docs/event-relay-service/README.md)** - Event publishing
- **[Docker Setup](./docs/docker/README.md)** - Container configuration
- **[Architecture](./docs/architecture/)** - System design patterns
- **[API Reference](./docs/api/)** - API documentation
- **[Learning Resources](./docs/learning/)** - Tutorials and guides

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests (including CDC and cache tests)
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🏆 Enterprise Features Showcase

This system demonstrates **enterprise-grade architecture patterns** including:

- **🔄 Change Data Capture (CDC)** for real-time data synchronization
- **⚡ Intelligent Caching** with Redis and CDC-driven invalidation
- **🛡️ Fault Tolerance** with Dead Letter Queues and retry mechanisms
- **📊 Comprehensive Monitoring** with health checks and metrics
- **🏗️ Microservices Architecture** with independent databases
- **🔄 Event-Driven Design** with Kafka messaging
- **📦 Dual Event Publishing** strategies (Outbox + CDC)
- **🔧 Production-Ready** deployment with Docker and monitoring

Perfect for demonstrating advanced system design skills to software engineering teams! 🚀
