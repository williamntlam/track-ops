# Event Relay Service

A dedicated microservice responsible for reading outbox events from the Order Service database and publishing them to Kafka. This service implements the **Change Data Capture (CDC)** pattern and serves as the **Event Publisher** in the TrackOps microservices architecture.

## 🏗️ Architecture

The Event Relay Service follows **Hexagonal Architecture** principles and implements the **Outbox Pattern** for reliable event publishing.

### Key Components

- **Domain Layer**: Outbox event models and business logic
- **Application Layer**: Event processing and scheduling services
- **Infrastructure Layer**: Database access and Kafka publishing
- **Ports & Adapters**: Clean interfaces for external dependencies

## 🚀 Features

### Core Functionality
- **Outbox Event Processing**: Reads unprocessed events from the outbox table
- **Kafka Publishing**: Publishes events to appropriate Kafka topics
- **Retry Mechanism**: Handles failed event publishing with exponential backoff
- **Event Cleanup**: Removes old processed events to prevent table bloat
- **Health Monitoring**: Comprehensive health checks and metrics

### Event Processing
- **Consumes**: Outbox events from Order Service database
- **Publishes**: All order events to Kafka topics
- **Topics**: `ORDER_CREATED`, `ORDER_STATUS_UPDATED`, `ORDER_DELIVERED`, `ORDER_CANCELLED`

### Data Management
- **PostgreSQL**: Reads from Order Service outbox_events table
- **Kafka**: Publishes events to message broker
- **Flyway**: Database schema validation

## 📊 Data Model

### Outbox Events (Read-Only)
```sql
-- This service reads from the outbox_events table in the Order Service database
SELECT * FROM outbox_events 
WHERE processed = false 
ORDER BY created_at ASC;
```

## 🔄 Event Flow

### Event Publishing Flow
```
1. Order Service → Creates order → Saves to outbox_events table
2. Event Relay Service → Polls outbox_events table
3. Event Relay Service → Reads unprocessed events
4. Event Relay Service → Publishes to Kafka topics
5. Event Relay Service → Marks events as processed
6. Downstream Services → Consume events from Kafka
```

### Retry Flow
```
1. Event Relay Service → Attempts to publish event
2. If failure → Increments retry count
3. If retry count < max_retries → Retry later
4. If retry count >= max_retries → Mark as failed
5. Cleanup → Remove old processed events
```

## 🛠️ Technology Stack

- **Java 21**: Programming language
- **Spring Boot 3.5.3**: Application framework
- **Spring Data JPA**: Data access layer
- **Spring Kafka**: Event streaming
- **PostgreSQL**: Source database (Order Service)
- **Flyway**: Database migrations
- **Docker**: Containerization
- **Kubernetes**: Orchestration

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Docker & Docker Compose
- PostgreSQL 15+ (Order Service database)
- Apache Kafka 3.6+

### Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd track-ops/event-relay-service
   ```

2. **Start dependencies**
   ```bash
   docker-compose up -d postgres kafka
   ```

3. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

4. **Verify health**
   ```bash
   curl http://localhost:8082/actuator/health
   ```

### Docker Deployment

1. **Build the image**
   ```bash
   docker build -t trackops/event-relay-service:latest .
   ```

2. **Run with Docker Compose**
   ```bash
   docker-compose up event-relay-service
   ```

### Kubernetes Deployment

1. **Apply configurations**
   ```bash
   kubectl apply -f k8s/event-relay-service.yaml
   ```

2. **Check deployment status**
   ```bash
   kubectl get pods -l app=event-relay-service
   kubectl get services event-relay-service
   ```

## 📡 API Endpoints

### Health Check
- **GET** `/actuator/health` - Service health status
- **GET** `/actuator/info` - Service information
- **GET** `/actuator/metrics` - Application metrics

### Event Management
- **GET** `/api/v1/event-relay/stats` - Get processing statistics
- **GET** `/api/v1/event-relay/events/unprocessed` - List unprocessed events
- **GET** `/api/v1/event-relay/events/{eventId}` - Get specific event
- **POST** `/api/v1/event-relay/events/{eventId}/process` - Manually process event
- **POST** `/api/v1/event-relay/process-all` - Process all unprocessed events

## 🔧 Configuration

### Application Properties
```properties
# Database (Order Service Database)
spring.datasource.url=jdbc:postgresql://localhost:5432/trackops
spring.datasource.username=postgres
spring.datasource.password=password

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3

# Event Relay Configuration
event-relay.batch-size=10
event-relay.polling-interval=5000
event-relay.max-retries=3
event-relay.retry-delay=1000
event-relay.cleanup.enabled=true
event-relay.cleanup.retention-hours=24
```

## 📈 Monitoring

### Health Indicators
- **Database Health**: PostgreSQL connectivity
- **Kafka Health**: Message broker connectivity
- **Event Processing**: Processing statistics and backlog
- **Error Rates**: Failed event processing rates

### Metrics
- **Event Processing Rate**: Events processed per minute
- **Backlog Size**: Number of unprocessed events
- **Error Rate**: Percentage of failed events
- **Processing Time**: Average time to process events

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
   - Verify connection string points to Order Service database
   - Ensure outbox_events table exists

2. **Kafka Connection Failed**
   - Check Kafka is running
   - Verify bootstrap servers configuration
   - Ensure topics are created

3. **No Events Being Processed**
   - Check if Order Service is creating outbox events
   - Verify polling interval configuration
   - Check for database connection issues

### Logs
```bash
# Docker logs
docker logs trackops-event-relay-service

# Kubernetes logs
kubectl logs -l app=event-relay-service
```

## 📚 Development

### Project Structure
```
event-relay-service/
├── src/main/java/com/trackops/eventrelay/
│   ├── domain/                    # Domain models
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

### Adding New Event Types

1. **Update Kafka Config**: Add new topic definitions
2. **Update Event Processing**: Handle new event types in EventRelayService
3. **Add Monitoring**: Include new events in health checks and metrics

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
