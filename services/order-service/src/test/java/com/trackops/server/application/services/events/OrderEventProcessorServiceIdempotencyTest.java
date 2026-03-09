package com.trackops.server.application.services.events;

import com.trackops.server.domain.model.CacheOperationResult;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.events.orders.OrderEvent;
import com.trackops.server.domain.model.enums.OrderStatus;
import com.trackops.server.ports.output.cache.IdempotencyCachePort;
import com.trackops.server.ports.output.inventory.InventoryReservationRequestPort;
import com.trackops.server.ports.output.persistence.events.ProcessedEventRepository;
import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Simulates: crash after DB write but before Kafka ACK, then redelivery.
 * Upon "restart", the redelivered message must be handled without creating duplicate records.
 * Uses only test-side mocks (no production code changes).
 */
@ExtendWith(MockitoExtension.class)
class OrderEventProcessorServiceIdempotencyTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private IdempotencyCachePort idempotencyCachePort;

    @Mock
    private InventoryReservationRequestPort inventoryReservationRequestPort;

    private OrderEventProcessorService processor;

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        processor = new OrderEventProcessorService(
                orderRepository,
                processedEventRepository,
                idempotencyCachePort,
                inventoryReservationRequestPort
        );
    }

    @Test
    @DisplayName("First delivery: DB write succeeds (insert=1); second delivery: idempotent skip (insert=0), no duplicate records")
    void redeliveredMessageDoesNotCreateDuplicateRecords() {
        // Same logical message delivered twice — use mock so we control eventId/orderId without touching production code
        OrderEvent firstDelivery = mockOrderEvent(EVENT_ID, ORDER_ID, "ORDER_CREATED");
        OrderEvent redelivery = mockOrderEvent(EVENT_ID, ORDER_ID, "ORDER_CREATED");

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // First call: insert claims the event (1 row). Second call: already processed (0 rows) — simulates redelivery after crash
        when(processedEventRepository.insertOnConflictDoNothing(
                eq(EVENT_ID.toString()),
                eq(ORDER_ID.toString()),
                eq("ORDER_CREATED"),
                any(),
                eq(0L)))
                .thenReturn(1)
                .thenReturn(0);

        when(idempotencyCachePort.markEventProcessed(any(), any())).thenReturn(CacheOperationResult.success());

        // First delivery (DB write commits, then "crash" before Kafka ACK)
        processor.processOrderEvent(firstDelivery);

        // "Restart" and redelivery
        processor.processOrderEvent(redelivery);

        // insertOnConflictDoNothing called twice with same eventId
        ArgumentCaptor<String> eventIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(processedEventRepository, times(2)).insertOnConflictDoNothing(
                eventIdCaptor.capture(),
                eq(ORDER_ID.toString()),
                eq("ORDER_CREATED"),
                any(),
                eq(0L));
        assertThat(eventIdCaptor.getAllValues()).containsExactly(EVENT_ID.toString(), EVENT_ID.toString());

        // Order updated (save) only once — redelivery must not re-apply
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderRepository, times(1)).findById(ORDER_ID);
    }

    @Test
    @DisplayName("When insert returns 0 (redelivery), handleEventByType and inventory request are not invoked again")
    void whenInsertReturnsZeroOnlyFirstDeliveryTriggersHandlerAndInventory() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        when(processedEventRepository.insertOnConflictDoNothing(any(), any(), any(), any(), any()))
                .thenReturn(1)
                .thenReturn(0);

        when(idempotencyCachePort.markEventProcessed(any(), any())).thenReturn(CacheOperationResult.success());

        OrderEvent first = mockOrderEvent(EVENT_ID, ORDER_ID, "ORDER_CREATED");
        OrderEvent second = mockOrderEvent(EVENT_ID, ORDER_ID, "ORDER_CREATED");

        processor.processOrderEvent(first);
        processor.processOrderEvent(second);

        verify(inventoryReservationRequestPort, times(1)).requestReservation(ORDER_ID);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("processOrderEvent throws when event is null")
    void processOrderEventThrowsWhenEventNull() {
        assertThatThrownBy(() -> processor.processOrderEvent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("processOrderEvent throws when eventId is null")
    void processOrderEventThrowsWhenEventIdNull() {
        OrderEvent event = mockOrderEvent(null, ORDER_ID, "ORDER_CREATED");
        assertThatThrownBy(() -> processor.processOrderEvent(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event ID");
    }

    @Test
    @DisplayName("processOrderEvent throws when orderId is null")
    void processOrderEventThrowsWhenOrderIdNull() {
        OrderEvent event = mockOrderEvent(EVENT_ID, null, "ORDER_CREATED");
        assertThatThrownBy(() -> processor.processOrderEvent(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order ID");
    }

    private static OrderEvent mockOrderEvent(UUID eventId, UUID orderId, String eventType) {
        OrderEvent event = mock(OrderEvent.class);
        when(event.getEventId()).thenReturn(eventId);
        when(event.getOrderId()).thenReturn(orderId);
        when(event.getEventType()).thenReturn(eventType);
        return event;
    }
}
