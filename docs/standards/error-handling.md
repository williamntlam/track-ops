# Error Handling Standards

## Exception Hierarchy

### Domain Exceptions
- **OrderNotFoundException**: When order doesn't exist
- **OrderValidationException**: When order data is invalid
- **InvalidOrderStatusTransitionException**: When status change is invalid

### Implementation
```java
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID orderId) {
        super("Order not found: " + orderId);
    }
}
```

## Error Handling Patterns

### 1. Fail Fast
- Validate inputs early
- Throw exceptions immediately for invalid data
- Don't continue with invalid state

### 2. Specific Exceptions
- Use specific exception types
- Include context in error messages
- Provide actionable error information

### 3. Logging Strategy
```java
try {
    // Business logic
} catch (OrderNotFoundException e) {
    log.warn("Order not found: {}", orderId);
    throw e; // Re-throw domain exceptions
} catch (Exception e) {
    log.error("Unexpected error processing order: {}", orderId, e);
    throw new RuntimeException("Failed to process order", e);
}
```

## Error Response Format

### REST API Errors
```json
{
  "error": "ORDER_NOT_FOUND",
  "message": "Order with ID 123e4567-e89b-12d3-a456-426614174000 not found",
  "timestamp": "2023-10-23T20:04:02.453Z",
  "path": "/api/orders/123e4567-e89b-12d3-a456-426614174000"
}
```

### Kafka Error Handling
```java
@KafkaListener
public void handleOrderCreated(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
    try {
        // Process message
        acknowledgment.acknowledge();
    } catch (JsonProcessingException e) {
        log.error("Invalid JSON format", e);
        acknowledgment.acknowledge(); // Skip malformed messages
    } catch (Exception e) {
        log.error("Processing failed", e);
        throw e; // Let Kafka retry
    }
}
```

## Database Error Handling

### Transaction Management
```java
@Transactional
public OrderResponse createOrder(CreateOrderRequest request) {
    try {
        // Business logic
        return orderResponse;
    } catch (Exception e) {
        // Transaction will rollback automatically
        throw new RuntimeException("Failed to create order", e);
    }
}
```

### Retry Logic
```java
@Retryable(value = {DataAccessException.class}, maxAttempts = 3)
public Order save(Order order) {
    return orderJpaRepository.save(order);
}
```

## Monitoring and Alerting

### Error Metrics
- Track exception rates by type
- Monitor failed operations
- Alert on critical errors

### Log Levels
- **ERROR**: System errors, exceptions
- **WARN**: Recoverable issues, business rule violations
- **INFO**: Important business events
- **DEBUG**: Detailed execution information

## Best Practices

### 1. Don't Swallow Exceptions
```java
// BAD
try {
    processOrder();
} catch (Exception e) {
    // Silent failure
}

// GOOD
try {
    processOrder();
} catch (Exception e) {
    log.error("Failed to process order", e);
    throw new RuntimeException("Order processing failed", e);
}
```

### 2. Use Appropriate Exception Types
```java
// BAD
throw new RuntimeException("Order not found");

// GOOD
throw new OrderNotFoundException(orderId);
```

### 3. Include Context
```java
// BAD
throw new OrderValidationException("Invalid data");

// GOOD
throw new OrderValidationException("Customer ID cannot be null for order: " + orderId);
```

### 4. Handle External Service Failures
```java
try {
    externalService.call();
} catch (ExternalServiceException e) {
    log.error("External service failed", e);
    // Implement fallback or retry logic
    return fallbackResponse();
}
```
