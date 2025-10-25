# Real-Time Order Tracking System – Caching, Kafka, Kubernetes Practice Project

## 🛠️ Project Summary

Build a scalable, real-time backend system inspired by services like Uber Eats or Amazon Logistics. Practice integrating caching strategies, Kafka for event streaming, and Kubernetes for deployment and orchestration.

---

## 🧱 Tech Stack

| Feature | Technology | Role |
| --- | --- | --- |
| Message Queue | Apache Kafka | Stream order status updates |
| Cache | Redis | Fast access to order status and delivery location |
| Database | PostgreSQL / MongoDB | Persistent order and user data |
| Backend API | Node.js / FastAPI / Spring Boot | REST/gRPC service for business logic |
| Frontend | React / Angular / Next.js | Real-time UI for tracking orders |
| Containerization | Docker + Kubernetes | Deployment and orchestration |
| Monitoring | Prometheus + Grafana | Metrics and visualization |
| Logging | ELK Stack or Loki | Centralized log collection and analysis |

---

## ⚙️ System Behavior

### 1. 🚚 Placing an Order

- Order written to DB
- Redis caches initial status (`PENDING`)
- Kafka emits event: `ORDER_CREATED`
- Inventory Service consumes event and reserves inventory
- Inventory Service publishes `INVENTORY_RESERVED` or `INVENTORY_RESERVATION_FAILED`
- Order Service updates status based on inventory response

### 2. 📦 Processing Orders

- Kafka consumer updates status to `PREPARING`, `IN_TRANSIT`, `DELIVERED`
- Each update:
    - Written to DB (write-through)
    - Updated in Redis cache
    - Emitted via Kafka topic `ORDER_STATUS_UPDATED`

### 3. 📲 Frontend Real-time Updates

- Frontend polls API or uses WebSocket
- Retrieves latest order status from Redis

### 4. 🏪 Microservices Architecture

- **Order Service** (Port 8080): Manages order lifecycle and business logic
- **Inventory Service** (Port 8081): Handles inventory reservations and releases  
- **Event Relay Service** (Port 8082): Change Data Capture (CDC) and event publishing
- **Event-Driven Communication**: Services communicate via Kafka events
- **Distributed Transactions**: SAGA pattern for complex workflows
- **Outbox Pattern**: Reliable event publishing with retry mechanisms

---

## 🧪 What You'll Practice

### 🔄 Caching Concepts

- Cache-aside and write-through caching
- Redis TTL and eviction strategy
- Preventing stale reads

### 🧵 Kafka Event Streaming

- Kafka producers and consumers
- Handling retries, backoff, DLQs
- Event replay support
- Change Data Capture (CDC) patterns

### 🏢 Microservices Architecture

- Service decomposition and domain boundaries
- Event-driven communication patterns
- Outbox pattern for reliable messaging
- Database per service pattern
- Service-to-service communication

### 🧊 Kubernetes + DevOps

- Deploy multiple microservices in Kubernetes
- Use Helm or Kustomize
- Set up liveness/readiness probes
- Auto-scaling services
- Service mesh and networking

### 🔍 Observability

- Prometheus metrics (cache hit rates, Kafka lag)
- Grafana dashboards
- Logging with ELK or Loki
- Distributed tracing across services

---

## 🧠 Optional Enhancements

- Rate limiting with Redis
- Circuit breaker using Resilience4j
- gRPC-based microservices (e.g. Delivery, Payment)
- Alerting with Prometheus Alertmanager

---

## 🔑 Key Takeaways

- Real-world caching strategies and cache invalidation
- Event-driven system design using Kafka
- State management across distributed services
- Deployment, monitoring, and scaling using Kubernetes

---

## 💡 Ideas for Expansion

- Add a delivery ETA prediction model
- Implement multi-region deployment with federation
- Integrate with payment processors (Stripe mock)
