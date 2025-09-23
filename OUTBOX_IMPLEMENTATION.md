# Outbox Pattern Implementation

This document describes the outbox pattern implementation in the TrackOps order tracking system for reliable event publishing.

## 🎯 Overview

The outbox pattern ensures **eventual consistency** between the database and Kafka by storing events in the database within the same transaction as the business data, then publishing them asynchronously.

## 🏗️ Architecture

### Core Components

1. **OutboxEvent** - Domain entity representing an event to be published
2. **OutboxEventRepository** - Repository for persisting outbox events
3. **OutboxEventService** - Service for creating and managing outbox events
4. **OutboxEventPublisher** - Scheduled service that publishes events to Kafka
5. **OutboxController** - REST API for monitoring and management

## 📋 How It Works

### 1. **Event Creation (Within Transaction)**
```java
@Transactional
public OrderResponse createOrder(CreateOrderRequest request) {
    // 1. Save order to database
    Order savedOrder = orderRepository.save(newOrder);
    
    // 2. Create outbox event (same transaction)
    OrderCreatedEvent event = new OrderCreatedEvent(savedOrder.getId(), "system");
    outboxEventService.createOrderEvent(
        savedOrder.getId().toString(),
        "ORDER_CREATED",
        event
    );
    
    // 3. Transaction commits - both order and event are saved
    return response;
}
```

### 2. **Event Publishing (Asynchronous)**
```java
@Scheduled(fixedDelay = 5000) // Every 5 seconds
public void processOutboxEvents() {
    List<OutboxEvent> unprocessedEvents = outboxEventRepository
        .findByProcessedFalseOrderByCreatedAtAsc();
    
    for (OutboxEvent event : unprocessedEvents) {
        // Publish to Kafka
        kafkaTemplate.send(event.getEventType(), key, event.getPayload());
        
        // Mark as processed
        event.markAsProcessed();
        outboxEventRepository.save(event);
    }
}
```

## 🗄️ Database Schema

### outbox_events Table
```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(255) NOT NULL,        -- Order ID
    event_type VARCHAR(100) NOT NULL,          -- ORDER_CREATED, ORDER_STATUS_UPDATED
    payload TEXT NOT NULL,                     -- JSON serialized event
    created_at TIMESTAMP NOT NULL,             -- When event was created
    processed BOOLEAN NOT NULL DEFAULT FALSE,  -- Whether published to Kafka
    processed_at TIMESTAMP,                    -- When published to Kafka
    retry_count INTEGER DEFAULT 0,             -- Number of retry attempts
    max_retries INTEGER DEFAULT 3,             -- Maximum retries allowed
    error_message VARCHAR(1000),               -- Last error message
    partition_key VARCHAR(255),                -- Kafka partition key
    version BIGINT DEFAULT 0                   -- Optimistic locking
);
```

## 🔄 Event Lifecycle

```
1. Business Operation Starts
   ↓
2. Save Business Data to Database
   ↓
3. Create Outbox Event (Same Transaction)
   ↓
4. Transaction Commits
   ↓
5. Outbox Publisher Picks Up Event
   ↓
6. Publish to Kafka
   ↓
7. Mark Event as Processed
```

## 🚀 Usage Examples

### Creating Outbox Events
```java
// In OrderService
@Transactional
public OrderResponse createOrder(CreateOrderRequest request) {
    // Save order
    Order savedOrder = orderRepository.save(newOrder);
    
    // Create outbox event
    OrderCreatedEvent event = new OrderCreatedEvent(savedOrder.getId(), "system");
    outboxEventService.createOrderEvent(
        savedOrder.getId().toString(),
        "ORDER_CREATED",
        event
    );
    
    return response;
}
```

### Monitoring Outbox Events
```java
// Get unprocessed events
List<OutboxEvent> unprocessed = outboxEventService.getUnprocessedEvents();

// Get events for specific order
List<OutboxEvent> orderEvents = outboxEventService.getEventsByAggregateId("order-123");

// Get statistics
OutboxEventStats stats = outboxEventService.getStats();
```

## 🌐 REST API

### Get Unprocessed Events
```
GET /api/outbox/events
```

### Get Events by Order ID
```
GET /api/outbox/events/aggregate/{orderId}
```

### Get Events by Type
```
GET /api/outbox/events/type/{eventType}
```

### Get Retryable Events
```
GET /api/outbox/events/retryable
```

### Process Specific Event
```
POST /api/outbox/events/{eventId}/process
```

### Get Statistics
```
GET /api/outbox/stats
```

### Cleanup Old Events
```
POST /api/outbox/cleanup
```

## ⚙️ Configuration

### Scheduled Tasks
```java
// Event publishing (every 5 seconds)
@Scheduled(fixedDelay = 5000)
public void processOutboxEvents()

// Cleanup old events (every hour)
@Scheduled(fixedDelay = 3600000)
public void cleanupProcessedEvents()
```

### Retry Logic
- **Max Retries**: 3 (configurable)
- **Retry Strategy**: Exponential backoff
- **Failed Events**: Marked as failed after max retries

## 🔧 Key Features

### 1. **Transactional Consistency**
- Events are created within the same transaction as business data
- Either both succeed or both fail
- No partial state issues

### 2. **Reliable Publishing**
- Automatic retry mechanism
- Dead letter handling for permanently failed events
- Idempotent publishing

### 3. **Monitoring & Management**
- REST API for monitoring
- Statistics and health checks
- Manual event processing

### 4. **Performance**
- Batch processing of events
- Efficient database queries
- Automatic cleanup of old events

## 🚨 Error Handling

### Retry Logic
```java
if (!event.canRetry()) {
    log.warn("Event {} exceeded max retries", event.getId());
    event.markAsFailed("Max retries exceeded");
    return;
}

try {
    kafkaTemplate.send(topic, key, payload);
    event.markAsProcessed();
} catch (Exception e) {
    event.markAsFailed(e.getMessage());
}
```

### Dead Letter Handling
- Events that exceed max retries are marked as failed
- Can be manually retried via API
- Monitoring alerts for failed events

## 📊 Monitoring

### Metrics to Track
- **Unprocessed Events**: Number of events waiting to be published
- **Processing Rate**: Events processed per minute
- **Failure Rate**: Percentage of events that fail
- **Retry Count**: Average retries per event

### Health Checks
```java
// Check outbox health
GET /api/outbox/stats
{
  "unprocessedCount": 5,
  "processedCount": 1250,
  "totalCount": 1255
}
```

## 🔮 Benefits

### 1. **Eventual Consistency**
- Database and Kafka are eventually consistent
- No data loss during failures
- Reliable event delivery

### 2. **Fault Tolerance**
- Survives system crashes
- Handles network partitions
- Automatic recovery

### 3. **Observability**
- Complete audit trail
- Event processing metrics
- Debugging capabilities

### 4. **Scalability**
- Asynchronous processing
- Batch operations
- Horizontal scaling

## 🆚 Before vs After

### Before (Direct Kafka Publishing)
```java
@Transactional
public OrderResponse createOrder(CreateOrderRequest request) {
    Order savedOrder = orderRepository.save(newOrder);
    
    // ❌ This can fail independently
    orderEventProducer.publishOrderCreated(event);
    
    return response;
}
```

**Problems:**
- Order saved but event not published
- Database and Kafka inconsistent
- No retry mechanism
- Difficult to debug

### After (Outbox Pattern)
```java
@Transactional
public OrderResponse createOrder(CreateOrderRequest request) {
    Order savedOrder = orderRepository.save(newOrder);
    
    // ✅ Same transaction
    outboxEventService.createOrderEvent(orderId, "ORDER_CREATED", event);
    
    return response;
}
```

**Benefits:**
- Atomic operation
- Guaranteed eventual consistency
- Automatic retry
- Complete audit trail

## 🧪 Testing

### Unit Tests
```java
@Test
public void testOutboxEventCreation() {
    // Test event creation
    OutboxEvent event = outboxEventService.createOrderEvent(
        "order-123", "ORDER_CREATED", orderCreatedEvent
    );
    
    assertThat(event.getAggregateId()).isEqualTo("order-123");
    assertThat(event.isProcessed()).isFalse();
}
```

### Integration Tests
```java
@Test
public void testEventPublishing() {
    // Create outbox event
    // Wait for scheduled processing
    // Verify event was published to Kafka
}
```

## 📚 References

- [Outbox Pattern - Microsoft](https://docs.microsoft.com/en-us/azure/architecture/patterns/outbox)
- [Reliable Event Publishing - Martin Fowler](https://martinfowler.com/articles/patterns-of-distributed-systems/outbox.html)
- [Event Sourcing and CQRS](https://martinfowler.com/eaaDev/EventSourcing.html)
