# TrackOps API Reference

Complete reference documentation for all TrackOps API endpoints.

## Base URL
```
https://api.trackops.com/v1
```

## Authentication
All requests require an API key in the header:
```
X-API-Key: your-api-key-here
```

## Orders API

### Create Order
**POST** `/orders`

Creates a new order in the system.

#### Request
```http
POST /orders
Content-Type: application/json
X-API-Key: your-api-key

{
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
}
```

#### Response
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "456e7890-e89b-12d3-a456-426614174001",
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "PENDING",
  "totalAmount": 99.99,
  "address": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA"
  },
  "deliveryInstructions": "Leave at front door",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

#### Parameters
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| customerId | UUID | Yes | Unique identifier for the customer |
| totalAmount | Decimal | Yes | Total order amount (must be > 0) |
| address | Address | Yes | Delivery address object |
| deliveryInstructions | String | No | Special delivery instructions |

### Get Order by ID
**GET** `/orders/{id}`

Retrieves a specific order by its ID.

#### Request
```http
GET /orders/456e7890-e89b-12d3-a456-426614174001
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "456e7890-e89b-12d3-a456-426614174001",
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "PENDING",
  "totalAmount": 99.99,
  "address": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA"
  },
  "deliveryInstructions": "Leave at front door",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

#### Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | UUID | Yes | Order ID (path parameter) |

### Update Order Status
**PUT** `/orders/{id}/status`

Updates the status of an existing order.

#### Request
```http
PUT /orders/456e7890-e89b-12d3-a456-426614174001/status
Content-Type: application/json
X-API-Key: your-api-key

{
  "newStatus": "CONFIRMED"
}
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "456e7890-e89b-12d3-a456-426614174001",
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "CONFIRMED",
  "totalAmount": 99.99,
  "address": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA"
  },
  "deliveryInstructions": "Leave at front door",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:35:00Z"
}
```

#### Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | UUID | Yes | Order ID (path parameter) |
| newStatus | String | Yes | New status (PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED) |

### Cancel Order
**DELETE** `/orders/{id}`

Cancels an existing order. Cannot cancel delivered orders.

#### Request
```http
DELETE /orders/456e7890-e89b-12d3-a456-426614174001
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 204 No Content
```

### Get All Orders
**GET** `/orders`

Retrieves a paginated list of all orders.

#### Request
```http
GET /orders?page=0&size=20&sort=createdAt,desc
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "content": [
    {
      "id": "456e7890-e89b-12d3-a456-426614174001",
      "customerId": "123e4567-e89b-12d3-a456-426614174000",
      "status": "PENDING",
      "totalAmount": 99.99,
      "address": { ... },
      "deliveryInstructions": "Leave at front door",
      "createdAt": "2024-01-15T10:30:00Z",
      "updatedAt": "2024-01-15T10:30:00Z"
    }
  ],
  "pageable": {
    "sort": {
      "sorted": true,
      "unsorted": false
    },
    "pageNumber": 0,
    "pageSize": 20,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 1,
  "size": 20,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false
  }
}
```

#### Query Parameters
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | Integer | No | 0 | Page number (0-based) |
| size | Integer | No | 20 | Page size |
| sort | String | No | - | Sort field and direction (e.g., `createdAt,desc`) |

### Get Orders by Status
**GET** `/orders/status/{status}`

Retrieves all orders with a specific status.

#### Request
```http
GET /orders/status/PENDING
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "456e7890-e89b-12d3-a456-426614174001",
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "status": "PENDING",
    "totalAmount": 99.99,
    "address": { ... },
    "deliveryInstructions": "Leave at front door",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  }
]
```

#### Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | String | Yes | Order status (PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED) |

### Get Orders by Customer
**GET** `/orders/customer/{customerId}`

Retrieves all orders for a specific customer.

#### Request
```http
GET /orders/customer/123e4567-e89b-12d3-a456-426614174000
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "456e7890-e89b-12d3-a456-426614174001",
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "status": "PENDING",
    "totalAmount": 99.99,
    "address": { ... },
    "deliveryInstructions": "Leave at front door",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  }
]
```

#### Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| customerId | UUID | Yes | Customer ID (path parameter) |

### Confirm Order
**POST** `/orders/{id}/confirm`

Confirms a pending order and starts the processing workflow.

#### Request
```http
POST /orders/456e7890-e89b-12d3-a456-426614174001/confirm
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "456e7890-e89b-12d3-a456-426614174001",
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "PENDING",
  "totalAmount": 99.99,
  "address": { ... },
  "deliveryInstructions": "Leave at front door",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### Process Order
**POST** `/orders/{id}/process`

Marks an order as being processed.

#### Request
```http
POST /orders/456e7890-e89b-12d3-a456-426614174001/process
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "456e7890-e89b-12d3-a456-426614174001",
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "PROCESSING",
  "totalAmount": 99.99,
  "address": { ... },
  "deliveryInstructions": "Leave at front door",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:40:00Z"
}
```

### Ship Order
**POST** `/orders/{id}/ship`

Marks an order as shipped.

#### Request
```http
POST /orders/456e7890-e89b-12d3-a456-426614174001/ship
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "456e7890-e89b-12d3-a456-426614174001",
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "SHIPPED",
  "totalAmount": 99.99,
  "address": { ... },
  "deliveryInstructions": "Leave at front door",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:45:00Z"
}
```

### Deliver Order
**POST** `/orders/{id}/deliver`

Marks an order as delivered.

#### Request
```http
POST /orders/456e7890-e89b-12d3-a456-426614174001/deliver
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "456e7890-e89b-12d3-a456-426614174001",
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "DELIVERED",
  "totalAmount": 99.99,
  "address": { ... },
  "deliveryInstructions": "Leave at front door",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:50:00Z"
}
```

## SAGA Management API

### Get SAGA Status
**GET** `/sagas/{sagaId}`

Retrieves the status of a specific SAGA instance.

#### Request
```http
GET /sagas/saga-123
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "saga-123",
  "orderId": "456e7890-e89b-12d3-a456-426614174001",
  "status": "COMPLETED",
  "currentStep": "ORDER_PROCESSED",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:35:00Z"
}
```

### Get SAGAs for Order
**GET** `/sagas/order/{orderId}`

Retrieves all SAGA instances associated with an order.

#### Request
```http
GET /sagas/order/456e7890-e89b-12d3-a456-426614174001
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "saga-123",
    "orderId": "456e7890-e89b-12d3-a456-426614174001",
    "status": "COMPLETED",
    "currentStep": "ORDER_PROCESSED",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:35:00Z"
  }
]
```

### Get SAGAs by Status
**GET** `/sagas/status/{status}`

Retrieves all SAGA instances with a specific status.

#### Request
```http
GET /sagas/status/COMPLETED
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "saga-123",
    "orderId": "456e7890-e89b-12d3-a456-426614174001",
    "status": "COMPLETED",
    "currentStep": "ORDER_PROCESSED",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:35:00Z"
  }
]
```

### Retry SAGA
**POST** `/sagas/{sagaId}/retry`

Retries a failed SAGA instance.

#### Request
```http
POST /sagas/saga-123/retry
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

"SAGA retry initiated successfully"
```

### Compensate SAGA
**POST** `/sagas/{sagaId}/compensate`

Triggers compensation for a SAGA instance.

#### Request
```http
POST /sagas/saga-123/compensate
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

"SAGA compensation initiated successfully"
```

## Outbox Events API

### Get Unprocessed Events
**GET** `/outbox/events`

Retrieves all unprocessed outbox events.

#### Request
```http
GET /outbox/events
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "event-123",
    "aggregateId": "456e7890-e89b-12d3-a456-426614174001",
    "eventType": "ORDER_CREATED",
    "eventData": "{ ... }",
    "status": "PENDING",
    "createdAt": "2024-01-15T10:30:00Z",
    "processedAt": null
  }
]
```

### Get Events by Aggregate ID
**GET** `/outbox/events/aggregate/{aggregateId}`

Retrieves all events for a specific aggregate.

#### Request
```http
GET /outbox/events/aggregate/456e7890-e89b-12d3-a456-426614174001
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "event-123",
    "aggregateId": "456e7890-e89b-12d3-a456-426614174001",
    "eventType": "ORDER_CREATED",
    "eventData": "{ ... }",
    "status": "PROCESSED",
    "createdAt": "2024-01-15T10:30:00Z",
    "processedAt": "2024-01-15T10:30:05Z"
  }
]
```

### Get Events by Type
**GET** `/outbox/events/type/{eventType}`

Retrieves all events of a specific type.

#### Request
```http
GET /outbox/events/type/ORDER_CREATED
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "event-123",
    "aggregateId": "456e7890-e89b-12d3-a456-426614174001",
    "eventType": "ORDER_CREATED",
    "eventData": "{ ... }",
    "status": "PROCESSED",
    "createdAt": "2024-01-15T10:30:00Z",
    "processedAt": "2024-01-15T10:30:05Z"
  }
]
```

### Get Retryable Events
**GET** `/outbox/events/retryable`

Retrieves all events that can be retried.

#### Request
```http
GET /outbox/events/retryable
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "event-124",
    "aggregateId": "456e7890-e89b-12d3-a456-426614174002",
    "eventType": "ORDER_STATUS_UPDATED",
    "eventData": "{ ... }",
    "status": "FAILED",
    "retryCount": 2,
    "createdAt": "2024-01-15T10:30:00Z",
    "processedAt": null
  }
]
```

### Get Event by ID
**GET** `/outbox/events/{eventId}`

Retrieves a specific event by its ID.

#### Request
```http
GET /outbox/events/event-123
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "event-123",
  "aggregateId": "456e7890-e89b-12d3-a456-426614174001",
  "eventType": "ORDER_CREATED",
  "eventData": "{ ... }",
  "status": "PROCESSED",
  "createdAt": "2024-01-15T10:30:00Z",
  "processedAt": "2024-01-15T10:30:05Z"
}
```

### Process Event
**POST** `/outbox/events/{eventId}/process`

Manually processes a specific event.

#### Request
```http
POST /outbox/events/event-123/process
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

"Event processed successfully"
```

### Get Outbox Statistics
**GET** `/outbox/stats`

Retrieves statistics about outbox events.

#### Request
```http
GET /outbox/stats
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "totalEvents": 1000,
  "pendingEvents": 5,
  "processedEvents": 990,
  "failedEvents": 5,
  "averageProcessingTime": 150
}
```

### Cleanup Old Events
**POST** `/outbox/cleanup`

Removes old processed events from the system.

#### Request
```http
POST /outbox/cleanup
X-API-Key: your-api-key
```

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

"Cleanup completed successfully"
```

## Error Responses

All error responses follow a consistent format:

```json
{
  "status": 400,
  "error": "Error Type",
  "message": "Human readable error message",
  "path": "/api/v1/orders",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Common Error Types

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

#### Internal Server Error
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please try again later."
}
```
