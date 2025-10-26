# Inventory Service

A microservice responsible for managing inventory reservations and releases in the TrackOps order management system.

## 🏗️ Architecture

The Inventory Service follows **Hexagonal Architecture** principles and implements the **Event-Driven Architecture** pattern using Apache Kafka for communication with other services.

### Key Components

- **Domain Layer**: Core business logic for inventory management
- **Application Layer**: Service orchestration and event processing
- **Infrastructure Layer**: Database, Kafka, and external integrations
- **Ports & Adapters**: Clean interfaces for external dependencies

## 🚀 Features

### Core Functionality
- **Inventory Management**: Track available and reserved quantities
- **Reservation System**: Reserve inventory for orders with expiration
- **Event Processing**: Consume order events and publish inventory events
- **Automatic Cleanup**: Release expired reservations

### Event Handling
- **Consumes**: `ORDER_CREATED`, `ORDER_CANCELLED`
- **Publishes**: `INVENTORY_RESERVED`, `INVENTORY_RESERVATION_FAILED`, `INVENTORY_RELEASED`

### Data Management
- **PostgreSQL**: Persistent storage for inventory and reservations
- **Redis**: Caching for performance optimization
- **Flyway**: Database migrations

## 📊 Data Model

### Inventory Items
```sql
CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    product_id VARCHAR(255) UNIQUE NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    available_quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Inventory Reservations
```sql
CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    reserved_at TIMESTAMP,
    expires_at TIMESTAMP,
    released_at TIMESTAMP
);
```

## 🔄 Event Flow

### Order Creation Flow
```
1. Order Service → ORDER_CREATED → Kafka
2. Inventory Service → Consumes ORDER_CREATED
3. Inventory Service → Reserves inventory
4. Inventory Service → INVENTORY_RESERVED → Kafka
5. Order Service → Consumes INVENTORY_RESERVED
6. Order Service → Updates order status to CONFIRMED
```

### Order Cancellation Flow
```
1. Order Service → ORDER_CANCELLED → Kafka
2. Inventory Service → Consumes ORDER_CANCELLED
3. Inventory Service → Releases reserved inventory
4. Inventory Service → INVENTORY_RELEASED → Kafka
5. Order Service → Consumes INVENTORY_RELEASED
```

## 🛠️ Technology Stack

- **Java 21**: Programming language
- **Spring Boot 3.5.3**: Application framework
- **Spring Data JPA**: Data access layer
- **Spring Kafka**: Event streaming
- **PostgreSQL**: Primary database
- **Redis**: Caching layer
- **Flyway**: Database migrations
- **Docker**: Containerization
- **Kubernetes**: Orchestration

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+
- Apache Kafka 3.6+

### Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd track-ops/inventory-service
   ```

2. **Start dependencies**
   ```bash
   docker-compose up -d postgres redis kafka
   ```

3. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

4. **Verify health**
   ```bash
   curl http://localhost:8081/actuator/health
   ```

### Docker Deployment

1. **Build the image**
   ```bash
   docker build -t trackops/inventory-service:latest .
   ```

2. **Run with Docker Compose**
   ```bash
   docker-compose up inventory-service
   ```

### Kubernetes Deployment

1. **Apply configurations**
   ```bash
   kubectl apply -f k8s/inventory-service.yaml
   ```

2. **Check deployment status**
   ```bash
   kubectl get pods -l app=inventory-service
   kubectl get services inventory-service
   ```

## 📡 API Endpoints

### Health Check
- **GET** `/actuator/health` - Service health status
- **GET** `/actuator/info` - Service information
- **GET** `/actuator/metrics` - Application metrics

### Inventory Management
- **GET** `/inventory/items` - List all inventory items
- **GET** `/inventory/items/{productId}` - Get specific item
- **GET** `/inventory/reservations` - List reservations
- **GET** `/inventory/reservations/order/{orderId}` - Get reservations for order

## 🔧 Configuration

### Application Properties
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/trackops_inventory
spring.datasource.username=postgres
spring.datasource.password=password

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=inventory-service

# Redis
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.database=1

# Inventory Service
inventory.service.retry.max-attempts=3
inventory.service.reservation.timeout-seconds=30
```

## 📈 Monitoring

### Health Indicators
- **Database Health**: PostgreSQL connectivity
- **Redis Health**: Cache connectivity
- **Kafka Health**: Message broker connectivity
- **Inventory Health**: Stock levels and availability

### Metrics
- **Inventory Levels**: Available vs reserved quantities
- **Reservation Success Rate**: Successful vs failed reservations
- **Event Processing**: Messages consumed and produced
- **Response Times**: API and database performance

### Prometheus Integration
```yaml
management.metrics.export.prometheus.enabled=true
management.endpoints.web.exposure.include=metrics
```

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew integrationTest
```

### End-to-End Tests
```bash
./gradlew e2eTest
```

## 🔍 Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check PostgreSQL is running
   - Verify connection string and credentials
   - Ensure database `trackops_inventory` exists

2. **Kafka Connection Failed**
   - Check Kafka is running
   - Verify bootstrap servers configuration
   - Ensure topics are created

3. **Redis Connection Failed**
   - Check Redis is running
   - Verify host and port configuration
   - Check Redis database number

### Logs
```bash
# Docker logs
docker logs trackops-inventory-service

# Kubernetes logs
kubectl logs -l app=inventory-service
```

## 📚 Development

### Project Structure
```
inventory-service/
├── src/main/java/com/trackops/inventory/
│   ├── domain/                    # Domain models and events
│   ├── application/               # Application services
│   ├── adapters/                  # Infrastructure adapters
│   ├── ports/                     # Port interfaces
│   └── config/                    # Configuration classes
├── src/main/resources/
│   ├── application.properties     # Configuration
│   └── db/migration/              # Database migrations
├── docker/                        # Docker configuration
├── k8s/                          # Kubernetes manifests
└── build.gradle                  # Build configuration
```

### Adding New Features

1. **Domain Events**: Add new events in `domain/events/`
2. **Services**: Implement business logic in `application/services/`
3. **Adapters**: Add infrastructure implementations in `adapters/`
4. **Ports**: Define interfaces in `ports/`
5. **Migrations**: Add database changes in `db/migration/`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the documentation
- Review the troubleshooting section
