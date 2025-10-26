# Complete Event Publishing Strategy Pattern

## 🎯 Overview

The system now uses a **clean Factory + Strategy pattern** that allows switching between:
- **Outbox Pattern** (Event Relay service + Outbox table)
- **Debezium CDC** (Database change capture via Debezium)

Only **ONE** strategy is active at a time based on configuration.

---

## 🔧 Architecture

### **Strategy Interface**
```java
public interface EventPublishingStrategy {
    void publishOrderCreated(Order order);
    void publishOrderStatusUpdated(Order order, String previousStatus);
    void publishOrderCancelled(Order order);
    boolean isEnabled();
}
```

### **Two Strategy Implementations**

#### **1. OutboxEventPublishingStrategy**
- **When enabled**: `app.event-publishing.strategy=outbox` (default)
- **How it works**: 
  - Saves events to `outbox_events` table
  - Event Relay service polls outbox and publishes to Kafka
  - Inventory service consumes from Kafka

#### **2. DebeziumEventPublishingStrategy**
- **When enabled**: `app.event-publishing.strategy=debezium`
- **How it works**:
  - Just logs (doesn't create outbox events)
  - Debezium captures database changes automatically
  - DebeziumOrderEventConsumer transforms CDC → application events
  - DebeziumRedisCacheConsumer invalidates cache
  - DebeziumRedisCacheWarmer updates cache with fresh data
  - Inventory service consumes from Kafka

### **Factory Service**
```java
@Service
public class EventPublishingService {
    private final List<EventPublishingStrategy> strategies;
    
    public void publishOrderCreated(Order order) {
        EventPublishingStrategy strategy = getEnabledStrategy();
        if (strategy != null) {
            strategy.publishOrderCreated(order);
        }
    }
    
    private EventPublishingStrategy getEnabledStrategy() {
        return strategies.stream()
            .filter(EventPublishingStrategy::isEnabled)
            .findFirst()
            .orElse(null);
    }
}
```

---

## 📊 Complete Process Flows

### **🔄 OUTBOX STRATEGY (Default)**

```
1. OrderService.createOrder()
   ↓
2. Database: Order saved to trackops_orders
   ↓
3. EventPublishingService → OutboxEventPublishingStrategy
   ↓
4. Outbox table: Event saved to outbox_events
   ↓
5. Event Relay Service: Polls outbox table
   ↓
6. Kafka: Publishes to ORDER_CREATED topic
   ↓
7. Inventory Service: Consumes ORDER_CREATED
   
[Debezium consumers: DISABLED]
```

### **🔄 DEBEZIUM STRATEGY**

```
1. OrderService.createOrder()
   ↓
2. Database: Order saved to trackops_orders
   ↓
3. EventPublishingService → DebeziumEventPublishingStrategy
   ↓
4. Debezium Connector: Captures database change
   ↓
5. Kafka: Publishes to trackops_orders.public.orders
   ↓
6a. DebeziumOrderEventConsumer: 
    - Consumes CDC event
    - Transforms to ORDER_CREATED
    - Publishes to ORDER_CREATED topic
   ↓
6b. DebeziumRedisCacheConsumer:
    - Invalidates cache entries
   ↓
6c. DebeziumRedisCacheWarmer:
    - Updates cache with fresh data
   ↓
7. Inventory Service: Consumes ORDER_CREATED
   
[Outbox table: NOT USED]
```

---

## ✅ Benefits

### **Separation of Concerns**
- ✅ Outbox pattern: Event Relay service handles outbox
- ✅ Debezium pattern: Database CDC handles events
- ✅ Only one strategy active at a time

### **Flexibility**
- ✅ Switch between strategies via configuration
- ✅ No code changes needed
- ✅ Both patterns remain functional

### **Cache Integration**
- ✅ With Debezium: Automatic cache invalidation + warming
- ✅ With Outbox: Manual cache management
- ✅ No redundant cache operations

---

## 🎛️ Configuration

### **Switch to Debezium Strategy**
```properties
app.event-publishing.strategy=debezium
```

### **Switch to Outbox Strategy (Default)**
```properties
app.event-publishing.strategy=outbox
```

### **Start Services**
```bash
# With Outbox strategy (default)
./start-infrastructure.sh
cd server && ./gradlew bootRun

# With Debezium strategy
cd server && ./gradlew bootRun --args='--app.event-publishing.strategy=debezium'
```

---

## 🔍 Verification

### **Outbox Strategy**
- ✅ Events appear in `outbox_events` table
- ✅ Event Relay service is active
- ✅ Debezium consumers are **disabled**

### **Debezium Strategy**
- ✅ No events in `outbox_events` table
- ✅ Debezium consumers are **active**
- ✅ Cache invalidation/warming is **active**
- ✅ Events flow through Kafka topics

---

## 📝 Key Changes Made

1. **OrderService** now uses `EventPublishingService` instead of `OutboxEventService`
2. **Strategy pattern** automatically selects the correct implementation
3. **Only ONE strategy active** at a time based on configuration
4. **Clean separation** between Outbox and Debezium patterns
5. **No redundant operations** - each strategy handles its own concerns

---

## 🎉 Summary

You now have a **clean factory + strategy pattern** that:
- ✅ Allows switching between Outbox and Debezium strategies
- ✅ Only runs the active strategy (no duplication)
- ✅ Integrates Redis cache with Debezium automatically
- ✅ Maintains backward compatibility with Outbox pattern
- ✅ Provides consistent event flow to downstream services
