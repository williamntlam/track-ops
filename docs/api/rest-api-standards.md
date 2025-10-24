# REST API Standards

## URL Design

### Resource-Based URLs
- Use nouns, not verbs
- Use plural forms for collections
- Use hierarchical structure for related resources

### Examples
```
GET    /api/orders                    # List all orders
GET    /api/orders/{id}               # Get specific order
POST   /api/orders                    # Create new order
PUT    /api/orders/{id}/status        # Update order status
DELETE /api/orders/{id}              # Delete order
```

## HTTP Methods

### GET
- Retrieve resources
- Idempotent and safe
- Use query parameters for filtering

### POST
- Create new resources
- Non-idempotent
- Include request body

### PUT
- Update entire resource
- Idempotent
- Include complete resource in body

### PATCH
- Partial updates
- Include only changed fields

### DELETE
- Remove resources
- Idempotent
- No request body needed

## Request/Response Format

### Request Headers
```
Content-Type: application/json
Accept: application/json
Authorization: Bearer <token>
```

### Response Headers
```
Content-Type: application/json
Location: /api/orders/123e4567-e89b-12d3-a456-426614174000
```

### Request Body Example
```json
{
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "totalAmount": 99.99,
  "address": {
    "streetAddress": "123 Main St",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "USA",
    "phoneNumber": "+1-555-123-4567"
  },
  "deliveryInstructions": "Leave at front door"
}
```

### Response Body Example
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "PENDING",
  "totalAmount": 99.99,
  "address": {
    "streetAddress": "123 Main St",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "USA",
    "phoneNumber": "+1-555-123-4567"
  },
  "deliveryInstructions": "Leave at front door",
  "createdAt": "2023-10-23T20:04:02.453Z",
  "updatedAt": "2023-10-23T20:04:02.453Z"
}
```

## Status Codes

### Success Codes
- **200 OK**: Successful GET, PUT, PATCH
- **201 Created**: Successful POST
- **204 No Content**: Successful DELETE

### Client Error Codes
- **400 Bad Request**: Invalid request data
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource not found
- **409 Conflict**: Resource conflict (e.g., duplicate)

### Server Error Codes
- **500 Internal Server Error**: Unexpected server error
- **503 Service Unavailable**: Service temporarily unavailable

## Validation

### Input Validation
```java
@PostMapping("/orders")
public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    // Validation handled by @Valid annotation
}
```

### Validation Annotations
```java
public class CreateOrderRequest {
    @NotNull
    private UUID customerId;
    
    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal totalAmount;
    
    @NotNull
    @Valid
    private AddressDTO address;
}
```

## Error Responses

### Standard Error Format
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Customer ID is required",
  "timestamp": "2023-10-23T20:04:02.453Z",
  "path": "/api/orders",
  "details": [
    {
      "field": "customerId",
      "message": "Customer ID cannot be null"
    }
  ]
}
```

## Pagination

### Query Parameters
```
GET /api/orders?page=0&size=20&sort=createdAt,desc
```

### Response Format
```json
{
  "content": [...],
  "pageable": {
    "sort": {
      "sorted": true,
      "unsorted": false
    },
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false,
  "numberOfElements": 20
}
```

## Filtering and Sorting

### Query Parameters
```
GET /api/orders?status=PENDING&customerId=123&sort=createdAt,desc
```

### Implementation
```java
@GetMapping("/orders")
public Page<OrderResponse> getAllOrders(
    @RequestParam(required = false) OrderStatus status,
    @RequestParam(required = false) UUID customerId,
    Pageable pageable) {
    // Implementation
}
```

## Security

### Authentication
- Use JWT tokens
- Include in Authorization header
- Validate on each request

### Authorization
- Check user permissions
- Validate resource ownership
- Use role-based access control

## Documentation

### OpenAPI/Swagger
- Document all endpoints
- Include request/response examples
- Specify error responses
- Use descriptive operation IDs

### API Versioning
- Use URL versioning: `/api/v1/orders`
- Include version in response headers
- Maintain backward compatibility
