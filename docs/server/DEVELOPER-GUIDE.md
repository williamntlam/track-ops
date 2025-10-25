# TrackOps Developer Guide

This guide provides comprehensive information for developers working on the TrackOps server application.

## 🚀 Getting Started

### Prerequisites
- **Java 17+** - OpenJDK or Oracle JDK
- **Gradle 7+** - Build tool
- **Docker** - For local development
- **Docker Compose** - For running dependencies
- **IDE** - IntelliJ IDEA, Eclipse, or VS Code

### Quick Setup

1. **Clone the repository**
```bash
git clone https://github.com/your-org/track-ops.git
cd track-ops/server
```

2. **Start dependencies**
```bash
docker-compose up -d postgres redis kafka
```

3. **Run the application**
```bash
./gradlew bootRun
```

4. **Verify setup**
```bash
curl http://localhost:8080/actuator/health
```

## 🏗️ Project Structure

```
server/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/trackops/server/
│   │   │       ├── adapters/           # External interfaces
│   │   │       │   ├── input/         # Inbound adapters
│   │   │       │   │   ├── web/       # REST controllers
│   │   │       │   │   └── messaging/ # Kafka consumers
│   │   │       │   └── output/        # Outbound adapters
│   │   │       │       ├── persistence/ # Database
│   │   │       │       ├── cache/     # Redis
│   │   │       │       └── events/    # Kafka producers
│   │   │       ├── application/       # Application services
│   │   │       │   └── services/     # Business logic
│   │   │       ├── domain/           # Core business logic
│   │   │       │   ├── model/        # Entities & Value Objects
│   │   │       │   ├── events/       # Domain events
│   │   │       │   └── exceptions/   # Domain exceptions
│   │   │       └── ports/            # Interfaces
│   │   │           ├── input/        # Use case interfaces
│   │   │           └── output/       # Repository interfaces
│   │   └── resources/
│   │       ├── application.yml       # Configuration
│   │       └── db/migration/         # Database migrations
│   └── test/
│       ├── java/                     # Test classes
│       └── resources/                # Test configuration
├── build.gradle                      # Build configuration
├── docker-compose.yml               # Local development
└── Dockerfile                       # Container image
```

## 🎯 Architecture Principles

### Hexagonal Architecture
The application follows the Hexagonal Architecture pattern:

- **Domain Layer** - Core business logic (entities, value objects, domain events)
- **Application Layer** - Use cases and application services
- **Adapters Layer** - External interfaces (web, database, messaging)
- **Ports** - Interfaces between layers

### Key Principles
1. **Dependency Inversion** - Depend on abstractions, not concretions
2. **Single Responsibility** - Each class has one reason to change
3. **Open/Closed** - Open for extension, closed for modification
4. **Interface Segregation** - Small, focused interfaces
5. **Dependency Injection** - Spring manages object lifecycle

## 🔧 Development Workflow

### 1. Feature Development

#### Create a New Feature
```bash
# Create feature branch
git checkout -b feature/order-tracking

# Make changes
# ... implement feature ...

# Run tests
./gradlew test

# Commit changes
git add .
git commit -m "feat: add order tracking functionality"

# Push and create PR
git push origin feature/order-tracking
```

#### Code Review Checklist
- [ ] Code follows project conventions
- [ ] Tests are included and passing
- [ ] Documentation is updated
- [ ] No breaking changes
- [ ] Performance considerations addressed

### 2. Testing Strategy

#### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        CreateOrderRequest request = createValidRequest();
        Order expectedOrder = createExpectedOrder();
        when(orderRepository.save(any(Order.class))).thenReturn(expectedOrder);
        
        // When
        OrderResponse response = orderService.createOrder(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(any(Order.class));
    }
}
```

#### Integration Tests
```java
@SpringBootTest
@Testcontainers
class OrderControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldCreateOrderViaApi() {
        // Given
        CreateOrderRequest request = createValidRequest();
        
        // When
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
            "/api/v1/orders", request, OrderResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
```

#### Test Configuration
```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/testdb
    username: test
    password: test
  jpa:
    hibernate:
      ddl-auto: create-drop
  redis:
    host: localhost
    port: 6379
  kafka:
    bootstrap-servers: localhost:9092
```

### 3. Database Development

#### Creating Migrations
```sql
-- src/main/resources/db/migration/V3__Add_order_items_table.sql
CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
```

#### Entity Development
```java
@Entity
@Table(name = "order_items")
public class OrderItem {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    // Constructors, getters, setters, business methods
}
```

### 4. API Development

#### Controller Development
```java
@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {
    
    private final OrderServicePort orderService;
    
    public OrderController(OrderServicePort orderService) {
        this.orderService = orderService;
    }
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }
}
```

#### DTO Development
```java
public class CreateOrderRequest {
    
    @NotNull(message = "Customer ID is required")
    private UUID customerId;
    
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    private BigDecimal totalAmount;
    
    @NotNull(message = "Address is required")
    @Valid
    private AddressDTO address;
    
    @Size(max = 500, message = "Delivery instructions cannot exceed 500 characters")
    private String deliveryInstructions;
    
    // Constructors, getters, setters
}
```

### 5. Event Development

#### Domain Event
```java
public class OrderCreatedEvent extends DomainEvent {
    
    private final UUID orderId;
    private final UUID customerId;
    private final BigDecimal totalAmount;
    
    public OrderCreatedEvent(UUID orderId, UUID customerId, BigDecimal totalAmount) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
    }
    
    // Getters
}
```

#### Event Producer
```java
@Component
public class OrderEventProducer {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("ORDER_CREATED", event.getOrderId().toString(), message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
```

#### Event Consumer
```java
@Component
@Slf4j
public class OrderEventConsumer {
    
    private final OrderEventProcessorPort orderEventProcessor;
    
    @KafkaListener(topics = "ORDER_CREATED", groupId = "trackops-orders")
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(record.value(), OrderCreatedEvent.class);
            orderEventProcessor.processOrderEvent(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize event", e);
        }
    }
}
```

## 🧪 Testing Guidelines

### Test Categories

#### 1. Unit Tests
- **Purpose**: Test individual components in isolation
- **Scope**: Single class or method
- **Dependencies**: Mocked using Mockito
- **Location**: `src/test/java`

#### 2. Integration Tests
- **Purpose**: Test component interactions
- **Scope**: Multiple classes working together
- **Dependencies**: Real or test containers
- **Location**: `src/test/java` (suffixed with `IntegrationTest`)

#### 3. Contract Tests
- **Purpose**: Test API contracts
- **Scope**: REST endpoints
- **Dependencies**: WireMock or similar
- **Location**: `src/test/java` (suffixed with `ContractTest`)

#### 4. End-to-End Tests
- **Purpose**: Test complete user workflows
- **Scope**: Entire application
- **Dependencies**: Full application stack
- **Location**: `src/test/java` (suffixed with `E2ETest`)

### Test Naming Conventions

```java
// Unit tests
class OrderServiceTest {
    
    @Test
    void shouldCreateOrderWhenValidRequestProvided() {
        // Test implementation
    }
    
    @Test
    void shouldThrowExceptionWhenCustomerIdIsNull() {
        // Test implementation
    }
}

// Integration tests
class OrderControllerIntegrationTest {
    
    @Test
    void shouldCreateOrderViaRestApi() {
        // Test implementation
    }
}
```

### Test Data Builders

```java
public class OrderTestDataBuilder {
    
    private UUID customerId = UUID.randomUUID();
    private BigDecimal totalAmount = new BigDecimal("99.99");
    private Address address = AddressTestDataBuilder.anAddress().build();
    private String deliveryInstructions = "Leave at front door";
    
    public static OrderTestDataBuilder anOrder() {
        return new OrderTestDataBuilder();
    }
    
    public OrderTestDataBuilder withCustomerId(UUID customerId) {
        this.customerId = customerId;
        return this;
    }
    
    public OrderTestDataBuilder withTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        return this;
    }
    
    public Order build() {
        return new Order(customerId, OrderStatus.PENDING, totalAmount, address, deliveryInstructions);
    }
}
```

## 🔍 Debugging

### Local Debugging

#### IntelliJ IDEA
1. Set breakpoints in your code
2. Run configuration with debug mode
3. Use step-through debugging
4. Inspect variables and call stack

#### VS Code
1. Install Java Extension Pack
2. Set breakpoints
3. Use F5 to start debugging
4. Use debug console for evaluation

### Remote Debugging

#### Enable Remote Debugging
```bash
# Add JVM options
JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

# Run application
java $JAVA_OPTS -jar app.jar
```

#### Connect from IDE
- **Host**: localhost
- **Port**: 5005
- **Transport**: Socket

### Logging

#### Log Levels
```yaml
logging:
  level:
    com.trackops: DEBUG          # Application logs
    org.springframework: INFO    # Framework logs
    org.hibernate: WARN          # Database logs
    org.apache.kafka: INFO       # Kafka logs
```

#### Structured Logging
```java
@Slf4j
public class OrderService {
    
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());
        
        try {
            Order order = orderRepository.save(newOrder);
            log.info("Order created successfully: {}", order.getId());
            return orderMapper.toResponse(order);
        } catch (Exception e) {
            log.error("Failed to create order for customer: {}", request.getCustomerId(), e);
            throw e;
        }
    }
}
```

## 📊 Performance Optimization

### Database Optimization

#### Query Optimization
```java
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    List<Order> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") UUID customerId);
    
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt >= :fromDate")
    List<Order> findByStatusAndCreatedAtAfter(
        @Param("status") OrderStatus status, 
        @Param("fromDate") Instant fromDate);
}
```

#### Indexing Strategy
```sql
-- Create indexes for frequently queried columns
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_customer_status ON orders(customer_id, status);
```

### Caching Optimization

#### Cache Configuration
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

#### Cache Usage
```java
@Service
public class OrderService {
    
    @Cacheable(value = "orders", key = "#orderId")
    public OrderResponse getOrderById(UUID orderId) {
        // Implementation
    }
    
    @CacheEvict(value = "orders", key = "#order.id")
    public OrderResponse updateOrder(Order order) {
        // Implementation
    }
}
```

### JVM Tuning

#### Production JVM Options
```bash
JAVA_OPTS="
-Xmx2g
-Xms1g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseContainerSupport
-XX:+UnlockExperimentalVMOptions
-XX:+UseCGroupMemoryLimitForHeap
"
```

## 🔒 Security Best Practices

### Input Validation
```java
@RestController
@Validated
public class OrderController {
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        // Implementation
    }
}
```

### SQL Injection Prevention
```java
// Use JPA methods (safe)
List<Order> orders = orderRepository.findByCustomerId(customerId);

// Use @Query with parameters (safe)
@Query("SELECT o FROM Order o WHERE o.customerId = :customerId")
List<Order> findByCustomerId(@Param("customerId") UUID customerId);

// Avoid string concatenation (unsafe)
// String query = "SELECT * FROM orders WHERE customer_id = '" + customerId + "'";
```

### Authentication & Authorization
```java
@RestController
@PreAuthorize("hasRole('USER')")
public class OrderController {
    
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'Order', 'READ')")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        // Implementation
    }
}
```

## 📚 Code Style Guidelines

### Naming Conventions
- **Classes**: PascalCase (e.g., `OrderService`)
- **Methods**: camelCase (e.g., `createOrder`)
- **Variables**: camelCase (e.g., `orderId`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)
- **Packages**: lowercase (e.g., `com.trackops.server`)

### Code Organization
```java
public class OrderService {
    
    // 1. Constants
    private static final int MAX_RETRY_COUNT = 3;
    
    // 2. Fields
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    
    // 3. Constructor
    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }
    
    // 4. Public methods
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Implementation
    }
    
    // 5. Private methods
    private void validateRequest(CreateOrderRequest request) {
        // Implementation
    }
}
```

### Documentation
```java
/**
 * Service for managing orders in the system.
 * 
 * This service handles the complete order lifecycle from creation to delivery,
 * including status updates, validation, and event publishing.
 * 
 * @author Your Name
 * @since 1.0.0
 */
@Service
public class OrderService {
    
    /**
     * Creates a new order in the system.
     * 
     * @param request the order creation request containing customer and order details
     * @return the created order response
     * @throws OrderValidationException if the request is invalid
     * @throws RuntimeException if the order cannot be saved
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Implementation
    }
}
```

## 🚀 Deployment

### Local Development
```bash
# Start dependencies
docker-compose up -d

# Run application
./gradlew bootRun

# Run tests
./gradlew test

# Build application
./gradlew build
```

### Production Deployment
```bash
# Build Docker image
docker build -t trackops-server:latest .

# Run with production configuration
docker run -d \
  --name trackops-server \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  trackops-server:latest
```

## 📞 Getting Help

### Resources
- **Documentation**: `/docs` folder
- **API Reference**: `/docs/API-REFERENCE.md`
- **Architecture**: `/docs/ARCHITECTURE.md`
- **Deployment**: `/docs/DEPLOYMENT.md`

### Support
- **Slack**: #trackops-dev
- **Email**: dev-team@trackops.com
- **Issues**: GitHub Issues
- **Wiki**: Internal wiki

### Code Review
- **Process**: All changes require PR review
- **Reviewers**: At least 2 team members
- **Automated Checks**: CI/CD pipeline must pass
- **Testing**: All tests must pass

## 🔄 Continuous Improvement

### Regular Activities
- **Code Reviews**: Weekly review sessions
- **Refactoring**: Monthly refactoring sprints
- **Performance Reviews**: Quarterly performance analysis
- **Security Audits**: Bi-annual security reviews

### Metrics to Track
- **Code Coverage**: > 80%
- **Build Time**: < 5 minutes
- **Test Execution**: < 2 minutes
- **Deployment Time**: < 10 minutes
- **Bug Rate**: < 5% of releases
