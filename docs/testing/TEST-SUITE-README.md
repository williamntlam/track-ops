# Java test suite

Unit and integration tests for the TrackOps server, organized by layer.

## Structure

| Package / class | What it tests |
|-----------------|----------------|
| **domain/model/orders/OrderTest** | Order status transitions (confirm, process, ship, deliver, cancel), invalid transitions, addOrderItem null |
| **application/services/events/OrderEventProcessorServiceIdempotencyTest** | Idempotency on redelivery (no duplicate records), validation (null event, eventId, orderId) |
| **application/services/dlq/DlqOrderServiceTest** | isOrderEventTopic, saveFailedOrderEvent (payload parsing, orderId extraction), findById |
| **adapters/input/web/exception/GlobalExceptionHandlerTest** | HTTP status and body for OrderNotFoundException, OrderValidationException, InvalidOrderStatusTransitionException, MethodArgumentNotValidException, HttpMessageNotReadableException, RuntimeException |
| **adapters/input/web/controllers/OrderControllerTest** | getOrderById (200/404), createOrder (201), cancelOrder (204), getAllOrders |
| **adapters/input/web/dto/mappers/OrderMapperTest** | orderToOrderResponse, createOrderRequestToOrder, address mappers, null safety |
| **adapters/input/web/validation/OrderStatusValidatorTest** | Valid status transitions from PENDING, CONFIRMED, DELIVERED; null handling |
| **adapters/input/messaging/DlqOrderErrorHandlerTest** | Returns null when DLQ save succeeds (ack); rethrows when DLQ save fails (no ack) |
| **adapters/output/messaging/inventory/OutboxInventoryReservationRequestAdapterTest** | requestReservation calls enqueueIfAbsent, idempotent when already enqueued |

## Running tests

```bash
./gradlew test
```

Single test class:

```bash
./gradlew test --tests "com.trackops.server.domain.model.orders.OrderTest"
```

## Conventions

- **JUnit 5** and **Mockito** for unit tests; no production code changes for test-only behavior (use mocks in test).
- **AssertJ** for assertions.
- **@DisplayName** used where it helps; method names are descriptive otherwise.
- Controllers are tested by calling methods with mocked ports (no MockMvc in this suite to avoid security setup).
