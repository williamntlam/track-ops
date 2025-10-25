# TrackOps Server API Documentation

Welcome to the TrackOps Server API documentation! This comprehensive guide covers everything you need to know about the TrackOps order management system.

## 📚 Table of Contents

- [API Overview](#api-overview)
- [Quick Start](#quick-start)
- [Authentication](#authentication)
- [API Endpoints](#api-endpoints)
- [Data Models](#data-models)
- [Error Handling](#error-handling)
- [Rate Limiting](#rate-limiting)
- [Examples](#examples)
- [SDKs](#sdks)
- [Changelog](#changelog)

## 🚀 API Overview

TrackOps is a comprehensive order management system built with Spring Boot, featuring:

- **Order Lifecycle Management** - Complete order processing from creation to delivery
- **Event-Driven Architecture** - Real-time updates via Kafka
- **High Performance** - Redis caching for optimal response times
- **Distributed Transactions** - SAGA pattern for complex workflows
- **Comprehensive Monitoring** - Health checks and metrics

### Base URL
```
Production: https://api.trackops.com/v1
Staging: https://staging-api.trackops.com/v1
Local: http://localhost:8080/api/v1
```

### API Version
Current version: **v1**

## ⚡ Quick Start

### 1. Create an Order
```bash
curl -X POST https://api.trackops.com/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "totalAmount": 99.99,
    "address": {
      "street": "123 Main St",
      "city": "New York",
      "state": "NY",
      "zipCode": "10001",
      "country": "USA"
    },
    "deliveryInstructions": "Leave at front door"
  }'
```

### 2. Get Order Status
```bash
curl https://api.trackops.com/v1/orders/123e4567-e89b-12d3-a456-426614174000
```

### 3. Update Order Status
```bash
curl -X PUT https://api.trackops.com/v1/orders/123e4567-e89b-12d3-a456-426614174000/status \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "CONFIRMED"}'
```

## 🔐 Authentication

Currently, the API uses API key authentication. Include your API key in the request header:

```bash
curl -H "X-API-Key: your-api-key-here" https://api.trackops.com/v1/orders
```

### Getting an API Key
Contact your system administrator to obtain an API key for your environment.

## 📋 API Endpoints

### Orders

#### Create Order
**POST** `/orders`

Creates a new order in the system.

**Request Body:**
```json
{
  "customerId": "uuid",
  "totalAmount": 99.99,
  "address": {
    "street": "string",
    "city": "string", 
    "state": "string",
    "zipCode": "string",
    "country": "string"
  },
  "deliveryInstructions": "string"
}
```

**Response:**
```json
{
  "id": "uuid",
  "customerId": "uuid",
  "status": "PENDING",
  "totalAmount": 99.99,
  "address": { ... },
  "deliveryInstructions": "string",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

#### Get Order by ID
**GET** `/orders/{id}`

Retrieves a specific order by its ID.

**Response:**
```json
{
  "id": "uuid",
  "customerId": "uuid", 
  "status": "PENDING",
  "totalAmount": 99.99,
  "address": { ... },
  "deliveryInstructions": "string",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

#### Update Order Status
**PUT** `/orders/{id}/status`

Updates the status of an existing order.

**Request Body:**
```json
{
  "newStatus": "CONFIRMED"
}
```

**Valid Status Values:**
- `PENDING` - Order created, awaiting confirmation
- `CONFIRMED` - Order confirmed and ready for processing
- `PROCESSING` - Order is being prepared
- `SHIPPED` - Order has been shipped
- `DELIVERED` - Order has been delivered
- `CANCELLED` - Order has been cancelled

#### Cancel Order
**DELETE** `/orders/{id}`

Cancels an existing order. Cannot cancel delivered orders.

#### Get All Orders
**GET** `/orders`

Retrieves a paginated list of all orders.

**Query Parameters:**
- `page` (optional) - Page number (default: 0)
- `size` (optional) - Page size (default: 20)
- `sort` (optional) - Sort field and direction (e.g., `createdAt,desc`)

#### Get Orders by Status
**GET** `/orders/status/{status}`

Retrieves all orders with a specific status.

#### Get Orders by Customer
**GET** `/orders/customer/{customerId}`

Retrieves all orders for a specific customer.

#### Confirm Order
**POST** `/orders/{id}/confirm`

Confirms a pending order and starts the processing workflow.

#### Process Order
**POST** `/orders/{id}/process`

Marks an order as being processed.

#### Ship Order
**POST** `/orders/{id}/ship`

Marks an order as shipped.

#### Deliver Order
**POST** `/orders/{id}/deliver`

Marks an order as delivered.

### SAGA Management

#### Get SAGA Status
**GET** `/sagas/{sagaId}`

Retrieves the status of a specific SAGA instance.

#### Get SAGAs for Order
**GET** `/sagas/order/{orderId}`

Retrieves all SAGA instances associated with an order.

#### Get SAGAs by Status
**GET** `/sagas/status/{status}`

Retrieves all SAGA instances with a specific status.

#### Retry SAGA
**POST** `/sagas/{sagaId}/retry`

Retries a failed SAGA instance.

#### Compensate SAGA
**POST** `/sagas/{sagaId}/compensate`

Triggers compensation for a SAGA instance.

### Outbox Events

#### Get Unprocessed Events
**GET** `/outbox/events`

Retrieves all unprocessed outbox events.

#### Get Events by Aggregate ID
**GET** `/outbox/events/aggregate/{aggregateId}`

Retrieves all events for a specific aggregate.

#### Get Events by Type
**GET** `/outbox/events/type/{eventType}`

Retrieves all events of a specific type.

#### Get Retryable Events
**GET** `/outbox/events/retryable`

Retrieves all events that can be retried.

#### Get Event by ID
**GET** `/outbox/events/{eventId}`

Retrieves a specific event by its ID.

#### Process Event
**POST** `/outbox/events/{eventId}/process`

Manually processes a specific event.

#### Get Outbox Statistics
**GET** `/outbox/stats`

Retrieves statistics about outbox events.

#### Cleanup Old Events
**POST** `/outbox/cleanup`

Removes old processed events from the system.

## 📊 Data Models

### Order
```json
{
  "id": "uuid",
  "customerId": "uuid",
  "status": "PENDING | CONFIRMED | PROCESSING | SHIPPED | DELIVERED | CANCELLED",
  "totalAmount": 99.99,
  "address": {
    "street": "string",
    "city": "string",
    "state": "string", 
    "zipCode": "string",
    "country": "string"
  },
  "deliveryInstructions": "string",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### Address
```json
{
  "street": "string",
  "city": "string",
  "state": "string",
  "zipCode": "string", 
  "country": "string"
}
```

### Error Response
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation failed",
  "path": "/api/v1/orders",
  "timestamp": "2024-01-15T10:30:00Z",
  "validationErrors": [
    {
      "field": "totalAmount",
      "rejectedValue": -100,
      "message": "Total amount must be greater than zero"
    }
  ]
}
```

## ❌ Error Handling

The API uses standard HTTP status codes and returns detailed error information:

### Status Codes
- `200` - Success
- `201` - Created
- `400` - Bad Request (validation errors)
- `404` - Not Found
- `409` - Conflict (business rule violations)
- `500` - Internal Server Error

### Error Response Format
All errors follow a consistent format:

```json
{
  "status": 400,
  "error": "Error Type",
  "message": "Human readable error message",
  "path": "/api/v1/orders",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Common Errors

#### Validation Errors
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation failed",
  "validationErrors": [
    {
      "field": "totalAmount",
      "rejectedValue": -100,
      "message": "Total amount must be greater than zero"
    }
  ]
}
```

#### Not Found
```json
{
  "status": 404,
  "error": "Order Not Found",
  "message": "Order not found with ID: 123e4567-e89b-12d3-a456-426614174000"
}
```

#### Business Rule Violations
```json
{
  "status": 409,
  "error": "Invalid Status Transition",
  "message": "Delivered orders cannot be cancelled"
}
```

## 🚦 Rate Limiting

The API implements rate limiting to ensure fair usage:

- **Standard Tier**: 1000 requests per hour
- **Premium Tier**: 10000 requests per hour
- **Enterprise Tier**: Unlimited

Rate limit headers are included in responses:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1642248000
```

## 💡 Examples

### Complete Order Workflow

1. **Create Order**
```bash
curl -X POST https://api.trackops.com/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "totalAmount": 99.99,
    "address": {
      "street": "123 Main St",
      "city": "New York", 
      "state": "NY",
      "zipCode": "10001",
      "country": "USA"
    }
  }'
```

2. **Confirm Order**
```bash
curl -X POST https://api.trackops.com/v1/orders/123e4567-e89b-12d3-a456-426614174000/confirm
```

3. **Process Order**
```bash
curl -X POST https://api.trackops.com/v1/orders/123e4567-e89b-12d3-a456-426614174000/process
```

4. **Ship Order**
```bash
curl -X POST https://api.trackops.com/v1/orders/123e4567-e89b-12d3-a456-426614174000/ship
```

5. **Deliver Order**
```bash
curl -X POST https://api.trackops.com/v1/orders/123e4567-e89b-12d3-a456-426614174000/deliver
```

### Pagination Example
```bash
curl "https://api.trackops.com/v1/orders?page=0&size=10&sort=createdAt,desc"
```

### Filter by Status
```bash
curl "https://api.trackops.com/v1/orders/status/PENDING"
```

## 🔧 SDKs

Official SDKs are available for:

- **Java** - Maven/Gradle packages
- **Python** - PyPI package
- **Node.js** - NPM package
- **C#** - NuGet package

### Java SDK Example
```java
import com.trackops.sdk.TrackOpsClient;
import com.trackops.sdk.models.Order;
import com.trackops.sdk.models.CreateOrderRequest;

TrackOpsClient client = new TrackOpsClient("your-api-key");

CreateOrderRequest request = CreateOrderRequest.builder()
    .customerId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
    .totalAmount(new BigDecimal("99.99"))
    .address(Address.builder()
        .street("123 Main St")
        .city("New York")
        .state("NY")
        .zipCode("10001")
        .country("USA")
        .build())
    .build();

Order order = client.orders().create(request);
```

## 📝 Changelog

### v1.0.0 (2024-01-15)
- Initial release
- Complete order management functionality
- Event-driven architecture
- Redis caching
- SAGA pattern implementation
- Comprehensive error handling

## 📞 Support

For API support and questions:

- **Email**: api-support@trackops.com
- **Documentation**: https://docs.trackops.com
- **Status Page**: https://status.trackops.com

## 📄 License

This API is proprietary software. Unauthorized use is prohibited.
