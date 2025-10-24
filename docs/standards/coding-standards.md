# Coding Standards - Learning Guide

## Why Coding Standards Matter

### 🎯 **The Goal**: Write code that is:
- **Readable**: Others (including future you) can understand it
- **Maintainable**: Easy to modify and extend
- **Testable**: Can be verified and debugged
- **Consistent**: Follows predictable patterns

## Java Coding Standards - The Why Behind Each Rule

### Package Structure
**Why**: Clear organization helps you and others find code quickly
```java
// GOOD: Clear separation of concerns
com.trackops.server.domain.model.orders.Order
com.trackops.server.ports.input.orders.OrderServicePort
com.trackops.server.adapters.input.web.controllers.OrderController
```

**Learning Exercise**: 
- Can you find the Order entity quickly?
- Can you tell what each package contains?
- What happens if you put everything in one package?

### Naming Conventions
**Why**: Consistent naming reduces cognitive load

```java
// GOOD: Names tell you what they do
public Order createOrder(CreateOrderRequest request)
public Optional<Order> findById(UUID orderId)
public boolean isOrderValid(Order order)

// BAD: Names don't explain purpose
public Order doThing(ThingRequest req)
public Optional<Order> get(UUID id)
public boolean check(Order o)
```

**Learning Questions**:
- What does `createOrder` do? (Creates a new order)
- What does `findById` do? (Finds an order by its ID)
- What does `isOrderValid` do? (Checks if order is valid)

### Method Design - Single Responsibility
**Why**: One method, one job makes code easier to understand and test

```java
// GOOD: Each method has one clear purpose
public OrderResponse createOrder(CreateOrderRequest request) {
    validateRequest(request);           // 1. Validate
    Order order = buildOrder(request); // 2. Build
    Order saved = saveOrder(order);    // 3. Save
    return mapToResponse(saved);       // 4. Map
}

// BAD: One method doing everything
public OrderResponse createOrder(CreateOrderRequest request) {
    // 50 lines of validation, building, saving, mapping, error handling...
}
```

**Learning Exercise**: 
- Can you test each step independently?
- What happens if validation fails?
- How would you modify just the validation logic?

### Error Handling - Why Specific Exceptions?
**Why**: Different errors need different handling

```java
// GOOD: Specific exceptions allow targeted handling
try {
    Order order = orderRepository.findById(orderId);
} catch (OrderNotFoundException e) {
    return ResponseEntity.notFound().build();  // 404
} catch (DatabaseConnectionException e) {
    return ResponseEntity.status(503).build(); // 503
}

// BAD: Generic exception doesn't help
try {
    Order order = orderRepository.findById(orderId);
} catch (Exception e) {
    return ResponseEntity.status(500).build(); // Always 500
}
```

**Learning Questions**:
- How would you handle a "not found" vs "database down" differently?
- What information does the client need in each case?
- How would you log these errors differently?

## Spring Boot Standards

### Annotations
- Use `@Service` for business logic
- Use `@Repository` for data access
- Use `@Component` for utility classes
- Use `@Transactional` for database operations

### Dependency Injection
- Prefer constructor injection over field injection
- Use `@RequiredArgsConstructor` from Lombok when appropriate
- Keep constructors simple and focused

### Configuration
- Use `@ConfigurationProperties` for external configuration
- Keep configuration classes focused
- Use profiles for environment-specific settings

## Database Standards

### Entity Design
- Use `@Entity` and `@Table` annotations
- Include proper `@Id` and `@GeneratedValue`
- Use `@Version` for optimistic locking
- Use `@Enumerated(EnumType.STRING)` for enums

### Repository Pattern
- Create interface in `ports` package
- Implement adapter in `adapters` package
- Use Spring Data JPA for simple queries
- Write custom queries when needed

## Testing Standards

### Unit Tests
- Test business logic in isolation
- Use mocks for dependencies
- Aim for high code coverage
- Use descriptive test names

### Integration Tests
- Test complete workflows
- Use `@SpringBootTest` for full context
- Test database interactions
- Test external service integrations
