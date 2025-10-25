# TrackOps Input Validation

This document describes the comprehensive input validation system implemented in TrackOps server for ensuring data integrity and API reliability.

## 🛡️ Validation Overview

TrackOps implements a multi-layered validation system that includes:
- **Bean Validation (JSR-303)** - Standard validation annotations
- **Custom Validators** - Business rule validation
- **Method Validation** - Parameter and return value validation
- **Global Exception Handling** - Consistent error responses

## 📋 Validation Rules

### **Order Creation (`CreateOrderRequest`)**

#### **Customer ID**
- ✅ **Required** - Cannot be null
- ✅ **Valid UUID** - Must be a valid UUID format
- ✅ **Customer Exists** - Validates against customer service (extensible)

#### **Total Amount**
- ✅ **Required** - Cannot be null
- ✅ **Minimum Value** - Must be at least $0.01
- ✅ **Maximum Value** - Cannot exceed $999,999.99
- ✅ **Decimal Precision** - Supports up to 2 decimal places

#### **Address**
- ✅ **Required** - Cannot be null
- ✅ **Nested Validation** - All address fields validated

#### **Delivery Instructions**
- ✅ **Optional** - Can be null or empty
- ✅ **Length Limit** - Maximum 500 characters
- ✅ **Character Set** - Only letters, numbers, spaces, and basic punctuation

### **Address Validation (`AddressDTO`)**

#### **Street Address**
- ✅ **Required** - Cannot be blank
- ✅ **Length** - 5-255 characters
- ✅ **Character Set** - Letters, numbers, spaces, hyphens, apostrophes

#### **City**
- ✅ **Required** - Cannot be blank
- ✅ **Length** - 2-100 characters
- ✅ **Character Set** - Letters, spaces, hyphens, apostrophes

#### **State**
- ✅ **Optional** - Can be null or empty
- ✅ **Length** - Maximum 100 characters
- ✅ **Character Set** - Letters, spaces, hyphens, apostrophes

#### **Postal Code**
- ✅ **Required** - Cannot be blank
- ✅ **Length** - 3-20 characters
- ✅ **Format** - Letters, numbers, spaces, hyphens

#### **Country**
- ✅ **Required** - Cannot be blank
- ✅ **Length** - 2-100 characters
- ✅ **Character Set** - Letters and spaces only

#### **Phone Number**
- ✅ **Optional** - Can be null or empty
- ✅ **Length** - 7-50 characters
- ✅ **Format** - Numbers, spaces, hyphens, parentheses, optional + prefix

### **Order Status Updates (`UpdateOrderStatusRequest`)**

#### **New Status**
- ✅ **Required** - Cannot be null
- ✅ **Valid Transition** - Must follow business rules

#### **Status Transition Rules**
```
PENDING → CONFIRMED, CANCELLED
CONFIRMED → PROCESSING, CANCELLED
PROCESSING → SHIPPED, CANCELLED
SHIPPED → DELIVERED, CANCELLED
DELIVERED → (No transitions - terminal state)
CANCELLED → (No transitions - terminal state)
```

### **Path Parameters**

#### **UUID Parameters**
- ✅ **Required** - Cannot be null
- ✅ **Valid Format** - Must be valid UUID

#### **String Parameters**
- ✅ **Required** - Cannot be blank
- ✅ **Length** - 1-50 characters
- ✅ **Character Set** - Letters, numbers, hyphens, underscores

## 🔧 Custom Validators

### **@ValidCustomerId**
Validates customer ID format and existence.

```java
@ValidCustomerId
private UUID customerId;
```

### **@ValidOrderStatus**
Validates order status transitions based on business rules.

```java
@ValidOrderStatus(message = "Invalid order status transition")
private OrderStatus newStatus;
```

## 📊 Validation Error Responses

### **Single Field Error**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/orders",
  "timestamp": "2024-01-15T10:30:00Z",
  "validationErrors": [
    {
      "field": "totalAmount",
      "message": "Total amount must be at least $0.01"
    }
  ]
}
```

### **Multiple Field Errors**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/orders",
  "timestamp": "2024-01-15T10:30:00Z",
  "validationErrors": [
    {
      "field": "customerId",
      "message": "Customer ID is required"
    },
    {
      "field": "address.streetAddress",
      "message": "Street address is required"
    },
    {
      "field": "address.postalCode",
      "message": "Postal code must be 3-20 characters and contain only letters, numbers, spaces, and hyphens"
    }
  ]
}
```

## 🚀 Usage Examples

### **Valid Order Creation**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "totalAmount": 99.99,
    "address": {
      "streetAddress": "123 Main Street",
      "city": "New York",
      "state": "NY",
      "postalCode": "10001",
      "country": "United States",
      "phoneNumber": "+1-555-123-4567"
    },
    "deliveryInstructions": "Leave at front door"
  }'
```

### **Invalid Order Creation (Validation Errors)**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": null,
    "totalAmount": -10.00,
    "address": {
      "streetAddress": "123",
      "city": "",
      "postalCode": "12",
      "country": "USA123"
    }
  }'
```

**Response:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "validationErrors": [
    {
      "field": "customerId",
      "message": "Customer ID is required"
    },
    {
      "field": "totalAmount",
      "message": "Total amount must be at least $0.01"
    },
    {
      "field": "address.streetAddress",
      "message": "Street address must be between 5 and 255 characters"
    },
    {
      "field": "address.city",
      "message": "City is required"
    },
    {
      "field": "address.postalCode",
      "message": "Postal code must be 3-20 characters and contain only letters, numbers, spaces, and hyphens"
    },
    {
      "field": "address.country",
      "message": "Country must be 2-100 characters and contain only letters and spaces"
    }
  ]
}
```

## 🔍 Validation Configuration

### **Application Properties**
```properties
# Validation configuration
spring.validation.enabled=true
spring.validation.fail-fast=false
```

### **Custom Validation Messages**
```properties
# Custom validation messages (optional)
validation.customer.id.required=Customer ID is required
validation.order.amount.min=Total amount must be at least $0.01
validation.address.street.required=Street address is required
```

## 🛠️ Extending Validation

### **Adding New Custom Validators**

1. **Create Annotation:**
```java
@Documented
@Constraint(validatedBy = MyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MyValidation {
    String message() default "Invalid value";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

2. **Create Validator:**
```java
public class MyValidator implements ConstraintValidator<MyValidation, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Validation logic
        return true;
    }
}
```

3. **Apply to DTOs:**
```java
@MyValidation
private String myField;
```

### **Adding Business Rule Validation**

1. **Extend CustomerIdValidator:**
```java
@Override
public boolean isValid(UUID customerId, ConstraintValidatorContext context) {
    // Check if customer exists in database
    boolean exists = customerService.exists(customerId);
    if (!exists) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("Customer does not exist")
               .addConstraintViolation();
        return false;
    }
    return true;
}
```

2. **Extend OrderStatusValidator:**
```java
// Add more complex business rules
private boolean canTransition(OrderStatus from, OrderStatus to) {
    // Check user permissions
    // Check order conditions
    // Check business rules
    return true;
}
```

## 📈 Performance Considerations

### **Validation Performance**
- ✅ **Fast Validation** - Most validations are O(1) operations
- ✅ **Lazy Loading** - Database validations only when needed
- ✅ **Caching** - Customer existence checks can be cached
- ✅ **Async Validation** - Complex validations can be async

### **Error Handling Performance**
- ✅ **Fail Fast** - Stop on first validation error (configurable)
- ✅ **Batch Validation** - Validate multiple fields together
- ✅ **Error Caching** - Cache common validation errors

## 🔒 Security Considerations

### **Input Sanitization**
- ✅ **XSS Prevention** - All string inputs are sanitized
- ✅ **SQL Injection Prevention** - Parameterized queries only
- ✅ **Path Traversal Prevention** - Path parameters validated
- ✅ **Size Limits** - All inputs have size constraints

### **Data Privacy**
- ✅ **PII Validation** - Sensitive data properly validated
- ✅ **Logging** - Validation errors logged without sensitive data
- ✅ **Audit Trail** - All validation failures tracked

## 🧪 Testing Validation

### **Unit Tests**
```java
@Test
void shouldValidateValidOrderRequest() {
    CreateOrderRequest request = createValidRequest();
    Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
}

@Test
void shouldRejectInvalidOrderRequest() {
    CreateOrderRequest request = createInvalidRequest();
    Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertEquals("Customer ID is required", violations.iterator().next().getMessage());
}
```

### **Integration Tests**
```java
@Test
void shouldReturnValidationErrorForInvalidRequest() throws Exception {
    String invalidJson = "{\"customerId\": null, \"totalAmount\": -10}";
    
    mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.validationErrors").isArray())
            .andExpect(jsonPath("$.validationErrors[0].field").value("customerId"));
}
```

## 📚 Best Practices

### **Validation Design**
- ✅ **Fail Fast** - Validate early and often
- ✅ **Clear Messages** - Provide helpful error messages
- ✅ **Consistent Rules** - Apply same rules across all endpoints
- ✅ **Business Logic** - Separate validation from business logic

### **Error Handling**
- ✅ **User-Friendly** - Error messages should help users fix issues
- ✅ **Developer-Friendly** - Include field names and validation rules
- ✅ **Consistent Format** - Use same error response structure
- ✅ **Proper HTTP Status** - Return appropriate status codes

### **Performance**
- ✅ **Efficient Validation** - Use appropriate validation annotations
- ✅ **Caching** - Cache expensive validation results
- ✅ **Async When Needed** - Use async validation for slow operations
- ✅ **Monitoring** - Track validation performance and errors
