# TrackOps - Real-Time Order Tracking System

A scalable microservices-based order tracking system built with Spring Boot, Kafka, Redis, and PostgreSQL.

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+
- Apache Kafka 3.6+

### Start the System

1. **Start Infrastructure**
   ```bash
   ./start-infrastructure.sh
   ```

2. **Start Microservices**
   ```bash
   # Server (Order Service)
   cd server && ./gradlew bootRun --args='--spring.profiles.active=docker'
   
   # Inventory Service
   cd inventory-service && ./gradlew bootRun --args='--spring.profiles.active=docker'
   
   # Event Relay Service
   cd event-relay-service && ./gradlew bootRun --args='--spring.profiles.active=docker'
   ```

3. **Access Services**
   - Server: http://localhost:8081
   - Inventory: http://localhost:8082
   - Event Relay: http://localhost:8083
   - Kafka UI: http://localhost:8080
   - pgAdmin: http://localhost:5050

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

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.2.0, Java 21
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Messaging**: Apache Kafka 3.6
- **Containerization**: Docker, Docker Compose
- **Monitoring**: Prometheus, Grafana

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
