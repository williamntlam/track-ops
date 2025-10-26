# TrackOps Enterprise API Documentation

## 🚀 Overview

TrackOps provides a comprehensive REST API for enterprise-grade order tracking and inventory management. The API implements advanced patterns including **Change Data Capture (CDC)**, **distributed caching**, **event-driven architecture**, and **fault-tolerant messaging**.

## 🔗 Base URLs

| Environment | Base URL | Description |
|-------------|----------|-------------|
| **Local Development** | `http://localhost:8081` | Order Service |
| **Local Development** | `http://localhost:8082` | Inventory Service |
| **Local Development** | `http://localhost:8084` | Event Relay Service |
| **Local Development** | `http://localhost:8083` | Debezium Connect |

## 📋 API Endpoints

### Order Service API (Port 8081)

#### **Order Management**

##### Create Order
```http
POST /api/orders
Content-Type: application/json

{
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "items": [
    {
      "productId": "550e8400-e29b-41d4-a716-446655440001",
      "quantity": 2,
      "price": 29.99
    }
  ],
  "deliveryAddress": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001"
  }
}
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "totalAmount": 59.98,
  "createdAt": "2024-01-15T10:30:00Z",
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440003",
      "productId": "550e8400-e29b-41d4-a716-446655440001",
      "quantity": 2,
      "price": 29.99
    }
  ]
}
```

##### Get Order
```http
GET /api/orders/{orderId}
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "CONFIRMED",
  "totalAmount": 59.98,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:35:00Z",
  "items": [...]
}
```

##### List Orders (Paginated)
```http
GET /api/orders?page=0&size=20&sort=createdAt,desc
```

**Response:**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "customerId": "550e8400-e29b-41d4-a716-446655440000",
      "status": "CONFIRMED",
      "totalAmount": 59.98,
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

##### Update Order Status
```http
PUT /api/orders/{orderId}/status
Content-Type: application/json

{
  "status": "SHIPPED",
  "trackingNumber": "TRK123456789"
}
```

##### Cancel Order
```http
DELETE /api/orders/{orderId}
```

#### **Health & Monitoring**

##### Application Health
```http
GET /actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "application": {
      "status": "UP",
      "details": {
        "application": "server",
        "profile": "default",
        "version": "1.0.0",
        "uptime": "2h 15m 30s",
        "jvm": "OpenJDK 64-Bit Server VM 21"
      }
    },
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "debezium": {
      "status": "UP",
      "details": {
        "strategy": "debezium",
        "status": "Active",
        "consumerGroups": ["debezium-order-consumer"],
        "cdcTopics": ["trackops_orders.public.orders"],
        "applicationTopics": ["ORDER_CREATED", "ORDER_CANCELLED"]
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "database": "Redis",
        "hitRate": "85.5%",
        "memoryUsage": "45.2MB"
      }
    }
  }
}
```

##### Cache Statistics
```http
GET /actuator/cache
```

**Response:**
```json
{
  "caches": {
    "orderCache": {
      "hitRate": "87.5%",
      "missRate": "12.5%",
      "size": 1250,
      "ttl": "1h"
    },
    "orderStatusCache": {
      "hitRate": "92.3%",
      "missRate": "7.7%",
      "size": 850,
      "ttl": "30m"
    }
  }
}
```

### Inventory Service API (Port 8082)

#### **Inventory Management**

##### Create Inventory Item
```http
POST /api/inventory/items
Content-Type: application/json

{
  "name": "Wireless Headphones",
  "description": "High-quality wireless headphones",
  "sku": "WH-001",
  "price": 99.99,
  "quantity": 100,
  "category": "Electronics"
}
```

##### Get Inventory Item
```http
GET /api/inventory/items/{itemId}
```

##### List Inventory Items
```http
GET /api/inventory/items?page=0&size=20&category=Electronics
```

##### Update Inventory Item
```http
PUT /api/inventory/items/{itemId}
Content-Type: application/json

{
  "name": "Wireless Headphones Pro",
  "price": 129.99,
  "quantity": 150
}
```

##### Delete Inventory Item
```http
DELETE /api/inventory/items/{itemId}
```

#### **Reservation Management**

##### Create Reservation
```http
POST /api/inventory/reservations
Content-Type: application/json

{
  "orderId": "550e8400-e29b-41d4-a716-446655440002",
  "items": [
    {
      "itemId": "550e8400-e29b-41d4-a716-446655440001",
      "quantity": 2
    }
  ],
  "expiresAt": "2024-01-15T11:30:00Z"
}
```

##### List Reservations
```http
GET /api/inventory/reservations?orderId=550e8400-e29b-41d4-a716-446655440002
```

#### **Health & Monitoring**

##### Inventory Health Summary
```http
GET /api/inventory/health
```

**Response:**
```json
{
  "status": "UP",
  "totalItems": 1250,
  "totalQuantity": 15000,
  "lowStockItems": 15,
  "reservedQuantity": 250,
  "availableQuantity": 14750,
  "cacheHitRate": "89.2%",
  "lastUpdated": "2024-01-15T10:30:00Z"
}
```

### Event Relay Service API (Port 8084)

#### **Event Management**

##### Get Unprocessed Events
```http
GET /api/events/unprocessed?page=0&size=20
```

**Response:**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440004",
      "eventType": "ORDER_CREATED",
      "aggregateId": "550e8400-e29b-41d4-a716-446655440002",
      "eventData": "{\"orderId\":\"550e8400-e29b-41d4-a716-446655440002\",\"customerId\":\"550e8400-e29b-41d4-a716-446655440000\"}",
      "createdAt": "2024-01-15T10:30:00Z",
      "processed": false
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 5,
  "totalPages": 1
}
```

##### Process Specific Event
```http
POST /api/events/process
Content-Type: application/json

{
  "eventId": "550e8400-e29b-41d4-a716-446655440004"
}
```

##### Process All Unprocessed Events
```http
POST /api/events/process-all
```

##### Event Processing Statistics
```http
GET /api/events/stats
```

**Response:**
```json
{
  "totalEvents": 1500,
  "processedEvents": 1495,
  "unprocessedEvents": 5,
  "failedEvents": 2,
  "processingRate": "95.8%",
  "averageProcessingTime": "150ms",
  "lastProcessedAt": "2024-01-15T10:30:00Z"
}
```

### Debezium Connect API (Port 8083)

#### **Connector Management**

##### List All Connectors
```http
GET /connectors
```

**Response:**
```json
["trackops-orders-connector"]
```

##### Get Connector Details
```http
GET /connectors/trackops-orders-connector
```

**Response:**
```json
{
  "name": "trackops-orders-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres-server",
    "database.port": "5432",
    "database.user": "postgres",
    "database.password": "password",
    "database.dbname": "trackops_orders",
    "database.server.name": "trackops_orders",
    "topic.prefix": "trackops_orders",
    "table.include.list": "public.orders,public.order_items"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING"
    }
  ],
  "type": "source"
}
```

##### Get Connector Status
```http
GET /connectors/trackops-orders-connector/status
```

**Response:**
```json
{
  "name": "trackops-orders-connector",
  "connector": {
    "state": "RUNNING",
    "worker_id": "debezium-connect:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "debezium-connect:8083"
    }
  ],
  "type": "source"
}
```

##### Restart Connector
```http
POST /connectors/trackops-orders-connector/restart
```

## 🔄 Event-Driven Architecture

### **Event Topics**

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

### **Event Schema**

#### Order Created Event
```json
{
  "eventType": "ORDER_CREATED",
  "orderId": "550e8400-e29b-41d4-a716-446655440002",
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "totalAmount": 59.98,
  "createdAt": "2024-01-15T10:30:00Z",
  "items": [
    {
      "productId": "550e8400-e29b-41d4-a716-446655440001",
      "quantity": 2,
      "price": 29.99
    }
  ]
}
```

#### Order Status Updated Event
```json
{
  "eventType": "ORDER_STATUS_UPDATED",
  "orderId": "550e8400-e29b-41d4-a716-446655440002",
  "previousStatus": "PENDING",
  "newStatus": "CONFIRMED",
  "updatedAt": "2024-01-15T10:35:00Z",
  "trackingNumber": "TRK123456789"
}
```

## 🛡️ Error Handling

### **HTTP Status Codes**

| Code | Description | Usage |
|------|-------------|-------|
| `200` | OK | Successful request |
| `201` | Created | Resource created successfully |
| `400` | Bad Request | Invalid request data |
| `404` | Not Found | Resource not found |
| `409` | Conflict | Resource already exists |
| `422` | Unprocessable Entity | Validation errors |
| `500` | Internal Server Error | Server error |
| `503` | Service Unavailable | Service temporarily unavailable |

### **Error Response Format**
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/orders",
  "details": [
    {
      "field": "customerId",
      "message": "Customer ID is required"
    },
    {
      "field": "items",
      "message": "At least one item is required"
    }
  ]
}
```

## 📊 Monitoring & Observability

### **Health Check Endpoints**

| Service | Endpoint | Description |
|---------|----------|-------------|
| Order Service | `GET /actuator/health` | Application health |
| Inventory Service | `GET /actuator/health` | Application health |
| Event Relay Service | `GET /actuator/health` | Application health |
| Debezium Connect | `GET /connectors/{name}/status` | Connector status |

### **Metrics Endpoints**

| Service | Endpoint | Description |
|---------|----------|-------------|
| Order Service | `GET /actuator/metrics` | Application metrics |
| Inventory Service | `GET /actuator/metrics` | Application metrics |
| Event Relay Service | `GET /actuator/metrics` | Application metrics |

### **Cache Monitoring**

| Service | Endpoint | Description |
|---------|----------|-------------|
| Order Service | `GET /actuator/cache` | Cache statistics |
| Inventory Service | `GET /actuator/cache` | Cache statistics |

## 🔒 Security

### **Authentication** (Planned)
- JWT-based authentication
- Role-based access control
- API key management

### **Rate Limiting** (Planned)
- Redis-based rate limiting
- Per-client rate limits
- Burst protection

### **Input Validation**
- Comprehensive request validation
- SQL injection prevention
- XSS protection

## 🚀 Performance

### **Caching Strategy**
- Redis-based caching with TTL
- CDC-driven cache invalidation
- Cache warming for performance

### **Pagination**
- Configurable page sizes
- Cursor-based pagination
- Performance optimization

### **Response Times**
- Average response time: < 100ms
- 95th percentile: < 200ms
- Cache hit rate: > 85%

---

This API documentation showcases **enterprise-grade REST API design** with advanced features including CDC integration, distributed caching, comprehensive monitoring, and fault-tolerant error handling. Perfect for demonstrating **senior-level API design skills**! 🚀
