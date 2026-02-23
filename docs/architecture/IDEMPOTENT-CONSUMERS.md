# Idempotent Kafka Consumers

## Implementation

- **Transactional processing**: `OrderEventProcessorService.processOrderEvent` is `@Transactional(rollbackFor = Exception.class)`. All DB work runs in one transaction.
- **Idempotent insert**: The first step is `INSERT INTO processed_events (...) ON CONFLICT (event_id) DO NOTHING`. If the row already exists (redelivery), the insert affects 0 rows and the method returns without running business logic again.
- **Kafka ACK after commit**: The consumer calls `orderEventProcessor.processOrderEvent(event)` then `acknowledgment.acknowledge()`. Because the processor is transactional, the transaction commits when the method returns. ACK is sent only after that, so a crash after DB commit but before ACK results in redelivery, and the redelivered message is handled idempotently (no duplicate `processed_events` row, no duplicate business updates).

## Validation: Crash after DB write, before ACK

1. Run the app and trigger processing of an order event (e.g. produce a message to `ORDER_CREATED`).
2. In `OrderEventProcessorService.processOrderEvent`, immediately after `handleEventByType(...)` (i.e. after the DB write), add `System.exit(1);` (or kill the container) so that the method never returns and Kafka never receives the ACK.
3. Restart the application. Kafka will redeliver the same message.
4. **Expected**: The consumer processes the redelivery; `insertOnConflictDoNothing` returns 0 (row already exists); the method returns early and does not run `handleEventByType` again. No duplicate row in `processed_events`, no duplicate order update. Then ACK is sent.
5. **Verify**: Query `processed_events` and `orders` and confirm there is a single row per event and no duplicate updates.
