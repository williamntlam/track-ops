# TrackOps End-to-End Test Guide

This guide provides a comprehensive test order to verify the complete TrackOps application flow from infrastructure startup through all microservices, CDC events, caching, and order lifecycle management.

## 📋 Test Order Overview

1. **System Startup & Health Checks**
2. **Inventory Setup**
3. **Order Creation & Flow**
4. **CDC Event Verification**
5. **Cache Integration Testing**
6. **Order Status Updates**
7. **Inventory Reservations**
8. **Order Cancellation**
9. **Monitoring & Verification**

---

## 🚀 Step 1: Start the System

```bash
# Start all infrastructure and microservices
./start-all-microservices.sh
```

**Expected Output:**
- ✅ Infrastructure services started (PostgreSQL, Redis, Kafka, Zookeeper)
- ✅ Debezium Connect running on port 8083
- ✅ Order Service running on port 8081
- ✅ Inventory Service running on port 8082
- ✅ Event Relay Service running on port 8084

**Wait 30-60 seconds** for all services to fully initialize.

---

## 🏥 Step 2: Verify System Health

### 2.1 Check All Service Health Endpoints

```bash
# Order Service Health
curl -s http://localhost:8081/actuator/health | jq '.'

# Inventory Service Health
curl -s http://localhost:8082/actuator/health | jq '.'

# Event Relay Service Health
curl -s http://localhost:8084/actuator/health | jq '.'

# Debezium Connect Status
curl -s http://localhost:8083/connectors | jq '.'
```

**Expected Results:**
- All services return `{"status":"UP"}` or similar healthy status
- Debezium connectors listed (typically `trackops-orders-connector`)

---

## 📦 Step 3: Setup Inventory Items

### 3.1 Create Inventory Items

```bash
# Create first inventory item
curl -X POST http://localhost:8082/api/inventory/items \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "PROD-001",
    "productName": "Wireless Headphones",
    "description": "High-quality wireless headphones with noise cancellation",
    "sku": "WH-001",
    "price": 99.99,
    "quantity": 100,
    "category": "Electronics"
  }' | jq '.'

# Save the item ID from response (let's call it ITEM_ID_1)

# Create second inventory item
curl -X POST http://localhost:8082/api/inventory/items \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "PROD-002",
    "productName": "Laptop Stand",
    "description": "Ergonomic aluminum laptop stand",
    "sku": "LS-001",
    "price": 49.99,
    "quantity": 50,
    "category": "Accessories"
  }' | jq '.'

# Save the item ID (ITEM_ID_2)

# Create third inventory item
curl -X POST http://localhost:8082/api/inventory/items \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "PROD-003",
    "productName": "USB-C Cable",
    "description": "USB-C to USB-C cable, 6ft",
    "sku": "CABLE-001",
    "price": 19.99,
    "quantity": 200,
    "category": "Accessories"
  }' | jq '.'
```

**Expected Results:**
- Each request returns 201 Created with the inventory item details
- Items have unique IDs and are stored in the inventory database

### 3.2 Verify Inventory Items

```bash
# List all inventory items
curl -s http://localhost:8082/api/inventory/items?page=0&size=20 | jq '.'

# Get specific item by product ID
curl -s http://localhost:8082/api/inventory/items/PROD-001 | jq '.'

# Check inventory health summary
curl -s http://localhost:8082/api/inventory/health | jq '.'
```

**Expected Results:**
- All 3 items visible in the list
- Health summary shows correct item counts and quantities

---

## 🛒 Step 4: Create Orders

### 4.1 Create First Order

```bash
# Create order with multiple items
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "550e8400-e29b-41d4-a716-446655440000",
    "items": [
      {
        "productId": "PROD-001",
        "quantity": 2,
        "price": 99.99
      },
      {
        "productId": "PROD-002",
        "quantity": 1,
        "price": 49.99
      }
    ],
    "deliveryAddress": {
      "street": "123 Main Street",
      "city": "New York",
      "state": "NY",
      "zipCode": "10001",
      "country": "USA"
    },
    "deliveryInstructions": "Leave at front door, ring doorbell"
  }' | jq '.'

# Save the order ID from response (ORDER_ID_1)
```

**Expected Results:**
- Order created with status `PENDING`
- Order ID returned
- Total amount calculated correctly (2 × 99.99 + 1 × 49.99 = 249.97)

### 4.2 Create Second Order

```bash
# Create another order
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "550e8400-e29b-41d4-a716-446655440001",
    "items": [
      {
        "productId": "PROD-003",
        "quantity": 5,
        "price": 19.99
      }
    ],
    "deliveryAddress": {
      "street": "456 Oak Avenue",
      "city": "Los Angeles",
      "state": "CA",
      "zipCode": "90001",
      "country": "USA"
    }
  }' | jq '.'

# Save order ID (ORDER_ID_2)
```

### 4.3 Verify Orders

```bash
# Get first order details
curl -s http://localhost:8081/api/orders/{ORDER_ID_1} | jq '.'

# List all orders (paginated)
curl -s "http://localhost:8081/api/orders?page=0&size=20&sort=createdAt,desc" | jq '.'
```

**Expected Results:**
- Orders retrieved successfully
- Status shows `PENDING`
- Items and totals are correct

---

## 🔄 Step 5: Verify CDC Event Flow

### 5.1 Check Debezium Connector Status

```bash
# List all connectors
curl -s http://localhost:8083/connectors | jq '.'

# Check specific connector status
curl -s http://localhost:8083/connectors/trackops-orders-connector/status | jq '.'

# Check connector tasks
curl -s http://localhost:8083/connectors/trackops-orders-connector/tasks | jq '.'
```

**Expected Results:**
- Connector is `RUNNING`
- Tasks show no errors

### 5.2 Monitor Kafka Topics

```bash
# Check raw Debezium CDC events (order changes)
docker exec trackops-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic trackops_orders.public.orders \
  --from-beginning \
  --max-messages 5 \
  --timeout-ms 10000

# Check application events (transformed)
docker exec trackops-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic ORDER_CREATED \
  --from-beginning \
  --max-messages 5 \
  --timeout-ms 10000
```

**Expected Results:**
- CDC events captured in Debezium topic
- Application events published to ORDER_CREATED topic

### 5.3 Verify Event Relay Service

```bash
# Check unprocessed events (if any)
curl -s "http://localhost:8084/api/events/unprocessed?page=0&size=20" | jq '.'

# Check event processing statistics
curl -s http://localhost:8084/api/events/stats | jq '.'
```

**Expected Results:**
- Events processed or queued for processing
- Statistics show event counts

---

## 💾 Step 6: Test Redis Cache Integration

### 6.1 Check Cache After Order Creation

```bash
# Check if order is cached
docker exec trackops-redis redis-cli KEYS "order:*" | head -10

# Get specific order from cache (replace with actual order ID)
docker exec trackops-redis redis-cli GET "order:entity:{ORDER_ID_1}"

# Check order status cache
docker exec trackops-redis redis-cli GET "order:status:{ORDER_ID_1}"

# Check cache TTL
docker exec trackops-redis redis-cli TTL "order:entity:{ORDER_ID_1}"
```

### 6.2 Check Cache Statistics

```bash
# Order Service cache stats
curl -s http://localhost:8081/actuator/cache | jq '.'

# Inventory Service cache stats
curl -s http://localhost:8082/actuator/cache | jq '.'
```

**Expected Results:**
- Orders cached with appropriate TTL values
- Cache statistics show hit/miss rates

### 6.3 Test Cache Invalidation (After Status Update)

```bash
# Monitor Redis in real-time (run this in a separate terminal)
docker exec trackops-redis redis-cli MONITOR | grep -E "(SET|DEL|EXPIRE).*order:"

# Then update an order status (see Step 7)
# Watch for cache invalidation operations
```

---

## 📊 Step 7: Update Order Status

### 7.1 Update Order Status to CONFIRMED

```bash
# Update first order status
curl -X PUT http://localhost:8081/api/orders/{ORDER_ID_1}/status \
  -H "Content-Type: application/json" \
  -d '{
    "newStatus": "CONFIRMED"
  }' | jq '.'

# Verify the update
curl -s http://localhost:8081/api/orders/{ORDER_ID_1} | jq '.status'
```

**Expected Results:**
- Status updated to `CONFIRMED`
- Order details reflect the change

### 7.2 Update Order Status to PROCESSING

```bash
curl -X PUT http://localhost:8081/api/orders/{ORDER_ID_1}/status \
  -H "Content-Type: application/json" \
  -d '{
    "newStatus": "PROCESSING"
  }' | jq '.'
```

### 7.3 Update Order Status to SHIPPED

```bash
curl -X PUT http://localhost:8081/api/orders/{ORDER_ID_1}/status \
  -H "Content-Type: application/json" \
  -d '{
    "newStatus": "SHIPPED"
  }' | jq '.'
```

### 7.4 Update Order Status to DELIVERED

```bash
curl -X PUT http://localhost:8081/api/orders/{ORDER_ID_1}/status \
  -H "Content-Type: application/json" \
  -d '{
    "newStatus": "DELIVERED"
  }' | jq '.'
```

**Expected Results:**
- Each status update succeeds
- Status transitions are valid
- Cache gets invalidated and refreshed (if monitoring)

---

## 📦 Step 8: Test Inventory Reservations

### 8.1 Create Reservation for Second Order

```bash
# Create reservation (replace ORDER_ID_2 with actual ID from Step 4.2)
curl -X POST http://localhost:8082/api/inventory/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "{ORDER_ID_2}",
    "items": [
      {
        "itemId": "{ITEM_ID_FOR_PROD-003}",
        "quantity": 5
      }
    ],
    "expiresAt": "2024-12-31T23:59:59Z"
  }' | jq '.'
```

**Expected Results:**
- Reservation created successfully
- Inventory quantity reduced or reserved

### 8.2 Verify Reservations

```bash
# List all reservations
curl -s http://localhost:8082/api/inventory/reservations | jq '.'

# List reservations for specific order
curl -s "http://localhost:8082/api/inventory/reservations?orderId={ORDER_ID_2}" | jq '.'
```

### 8.3 Verify Inventory Updated

```bash
# Check inventory item quantity (should reflect reservations)
curl -s http://localhost:8082/api/inventory/items/PROD-003 | jq '.quantity'

# Check inventory health (should show reserved quantity)
curl -s http://localhost:8082/api/inventory/health | jq '.'
```

**Expected Results:**
- Reservations visible in the system
- Inventory quantities adjusted appropriately
- Health summary shows reserved vs available quantities

---

## ❌ Step 9: Test Order Cancellation

### 9.1 Create Order for Cancellation Test

```bash
# Create a new order for cancellation
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "550e8400-e29b-41d4-a716-446655440002",
    "items": [
      {
        "productId": "PROD-001",
        "quantity": 1,
        "price": 99.99
      }
    ],
    "deliveryAddress": {
      "street": "789 Pine Road",
      "city": "Chicago",
      "state": "IL",
      "zipCode": "60601",
      "country": "USA"
    }
  }' | jq '.'

# Save order ID (ORDER_ID_3)
```

### 9.2 Cancel the Order

```bash
# Cancel the order
curl -X DELETE http://localhost:8081/api/orders/{ORDER_ID_3}

# Verify cancellation
curl -s http://localhost:8081/api/orders/{ORDER_ID_3} | jq '.status'
```

**Expected Results:**
- Order status updated to `CANCELLED`
- ORDER_CANCELLED event published to Kafka
- Inventory reservations released (if any)

### 9.3 Verify Cancellation Events

```bash
# Check for ORDER_CANCELLED events
docker exec trackops-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic ORDER_CANCELLED \
  --from-beginning \
  --max-messages 5 \
  --timeout-ms 10000
```

---

## 🔍 Step 10: Final Verification & Monitoring

### 10.1 Verify All Orders

```bash
# List all orders with different statuses
curl -s "http://localhost:8081/api/orders?page=0&size=50" | jq '.content[] | {id, status, customerId, totalAmount}'
```

**Expected Results:**
- Multiple orders visible with different statuses
- Includes PENDING, CONFIRMED, DELIVERED, and CANCELLED orders

### 10.2 Check Service Health Summary

```bash
# Order Service
curl -s http://localhost:8081/actuator/health | jq '.'

# Inventory Service
curl -s http://localhost:8082/actuator/health | jq '.'

# Event Relay Service
curl -s http://localhost:8084/actuator/health | jq '.'
```

### 10.3 Check Cache Performance

```bash
# Order Service cache stats
curl -s http://localhost:8081/actuator/cache | jq '.'

# Inventory Service cache stats
curl -s http://localhost:8082/actuator/cache | jq '.'
```

### 10.4 Verify Database Consistency

```bash
# Check Debezium connector lag
curl -s http://localhost:8083/connectors/trackops-orders-connector/status | jq '.connector.tasks[] | {id, state}'

# Check Kafka consumer groups
docker exec trackops-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list
```

### 10.5 View Service Logs

```bash
# Order Service logs
tail -n 50 logs/Order\ Service.log

# Inventory Service logs
tail -n 50 logs/Inventory\ Service.log

# Event Relay Service logs
tail -n 50 logs/Event\ Relay\ Service.log

# Check for any errors or warnings
grep -i "error\|exception\|warn" logs/*.log | tail -20
```

---

## ✅ Expected Test Results Summary

After completing all steps, you should verify:

1. **Infrastructure**: All services running and healthy
2. **Inventory**: Items created and accessible
3. **Orders**: Multiple orders created with various statuses
4. **CDC Events**: Debezium capturing database changes
5. **Kafka Events**: ORDER_CREATED and ORDER_CANCELLED events flowing
6. **Cache**: Redis caching orders with appropriate TTL
7. **Status Updates**: Order status transitions working correctly
8. **Reservations**: Inventory reservations created and tracked
9. **Cancellation**: Orders can be cancelled with events published
10. **Monitoring**: All health checks and cache stats available

---

## 🐛 Troubleshooting

### Services Not Starting
```bash
# Check Docker containers
docker compose ps

# Check service logs
docker compose logs [service-name]
```

### Debezium Connector Issues
```bash
# Restart connector
curl -X POST http://localhost:8083/connectors/trackops-orders-connector/restart

# Check connector configuration
curl -s http://localhost:8083/connectors/trackops-orders-connector/config | jq '.'
```

### Cache Not Working
```bash
# Check Redis connection
docker exec trackops-redis redis-cli PING

# Check Redis memory
docker exec trackops-redis redis-cli INFO memory
```

### Events Not Flowing
```bash
# Check Kafka topics
docker exec trackops-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check consumer lag
docker exec trackops-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --all-groups
```

---

## 🎯 Quick Test Script

For a quick verification, you can use this condensed version:

```bash
# 1. Health checks
curl -s http://localhost:8081/actuator/health | jq '.status'
curl -s http://localhost:8082/actuator/health | jq '.status'

# 2. Create inventory item
ITEM=$(curl -s -X POST http://localhost:8082/api/inventory/items \
  -H "Content-Type: application/json" \
  -d '{"productId":"TEST-001","productName":"Test Item","sku":"TEST","price":10.00,"quantity":100,"category":"Test"}' \
  | jq -r '.id')

# 3. Create order
ORDER=$(curl -s -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"test-customer","items":[{"productId":"TEST-001","quantity":1,"price":10.00}],"deliveryAddress":{"street":"123 Test","city":"Test","state":"TS","zipCode":"12345","country":"USA"}}' \
  | jq -r '.id')

# 4. Get order
curl -s http://localhost:8081/api/orders/$ORDER | jq '.status'

# 5. Update status
curl -s -X PUT http://localhost:8081/api/orders/$ORDER/status \
  -H "Content-Type: application/json" \
  -d '{"newStatus":"CONFIRMED"}' | jq '.status'

# 6. Verify final state
curl -s http://localhost:8081/api/orders/$ORDER | jq '{id, status, totalAmount}'
```

---

## 📝 Notes

- Replace `{ORDER_ID_1}`, `{ORDER_ID_2}`, etc. with actual order IDs from responses
- Replace `{ITEM_ID_1}`, `{ITEM_ID_2}`, etc. with actual item IDs from responses
- Wait a few seconds between operations to allow event propagation
- Use `jq '.'` for pretty-printed JSON output (install with `sudo apt-get install jq` if needed)
- Monitor logs in separate terminal windows for real-time debugging

---

**Happy Testing! 🚀**

