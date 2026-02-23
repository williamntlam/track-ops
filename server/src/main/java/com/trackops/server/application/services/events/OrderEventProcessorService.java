package com.trackops.server.application.services.events;

import com.trackops.server.domain.model.CacheOperationResult;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.events.orders.InventoryReleasedEvent;
import com.trackops.server.domain.events.orders.InventoryReservationFailedEvent;
import com.trackops.server.domain.events.orders.InventoryReservedEvent;
import com.trackops.server.domain.events.orders.OrderEvent;
import com.trackops.server.domain.model.enums.OrderStatus;
import com.trackops.server.ports.input.events.OrderEventProcessorPort;
import com.trackops.server.ports.output.cache.IdempotencyCachePort;
import com.trackops.server.ports.output.inventory.InventoryReservationRequestPort;
import com.trackops.server.ports.output.persistence.events.ProcessedEventRepository;
import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
public class OrderEventProcessorService implements OrderEventProcessorPort {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProcessorService.class);
    private static final String CONSUMER_GROUP = "trackops-orders";

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final IdempotencyCachePort idempotencyCachePort;
    private final InventoryReservationRequestPort inventoryReservationRequestPort;

    public OrderEventProcessorService(OrderRepository orderRepository, ProcessedEventRepository processedEventRepository, IdempotencyCachePort idempotencyCachePort, InventoryReservationRequestPort inventoryReservationRequestPort) {
        this.orderRepository = orderRepository;
        this.processedEventRepository = processedEventRepository;
        this.idempotencyCachePort = idempotencyCachePort;
        this.inventoryReservationRequestPort = inventoryReservationRequestPort;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processOrderEvent(OrderEvent event) {
        validateEvent(event);
        UUID eventId = event.getEventId();
        UUID orderId = event.getOrderId();

        // Idempotency: try to claim this event with ON CONFLICT DO NOTHING. If 0 rows, already processed (e.g. redelivery).
        int inserted = processedEventRepository.insertOnConflictDoNothing(
                eventId.toString(),
                orderId.toString(),
                event.getEventType(),
                CONSUMER_GROUP,
                0L);
        if (inserted == 0) {
            log.debug("Event already processed (idempotent skip): eventId={}", eventId);
            return;
        }

        // Handle event (only runs for first-time processing)
        Order processedOrder = handleEventByType(event, orderId);

        // Notify inventory so they can reserve; ack only after this succeeds (blocking until Kafka ack)
        if ("ORDER_CREATED".equals(event.getEventType())) {
            inventoryReservationRequestPort.requestReservation(orderId);
        }

        // Optional: mark in Redis for fast path on next time
        CacheOperationResult cacheResult = idempotencyCachePort.markEventProcessed(eventId, Duration.ofHours(24));
        if (cacheResult.isFailure()) {
            log.warn("Failed to mark event {} as processed in cache: {}", eventId, cacheResult.getErrorMessage());
        }
    }

    private void validateEvent(OrderEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Order event cannot be null");
        }
        
        if (event.getEventId() == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        
        if (event.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        
        if (event.getEventType() == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        
        log.debug("Event validation passed for event: {} of type: {}", event.getEventId(), event.getEventType());
    }

    private Order handleEventByType(OrderEvent event, UUID orderId) {
        switch (event.getEventType()) {
            case "ORDER_CREATED":
            case "ORDER_STATUS_UPDATED":
            case "ORDER_DELIVERED":
            case "ORDER_CANCELLED":
                // Standard order processing
                return orderRepository.findById(orderId)
                    .map(order -> {
                        order.process();
                        return orderRepository.save(order);
                    })
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
                    
            case "INVENTORY_RESERVED":
                return handleInventoryReserved((InventoryReservedEvent) event, orderId);
                
            case "INVENTORY_RESERVATION_FAILED":
                return handleInventoryReservationFailed((InventoryReservationFailedEvent) event, orderId);
                
            case "INVENTORY_RELEASED":
                return handleInventoryReleased((InventoryReleasedEvent) event, orderId);
                
            default:
                log.warn("Unknown event type: {}", event.getEventType());
                return orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        }
    }

    private Order handleInventoryReserved(InventoryReservedEvent event, UUID orderId) {
        log.info("Processing inventory reserved event for order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Update order status to CONFIRMED since inventory is reserved
        if (order.getStatus() == OrderStatus.PENDING) {
            order.confirm();
            order = orderRepository.save(order);
            log.info("Order {} confirmed after inventory reservation", orderId);
        }
        
        return order;
    }

    private Order handleInventoryReservationFailed(InventoryReservationFailedEvent event, UUID orderId) {
        log.warn("Processing inventory reservation failed event for order: {}", orderId);
        log.warn("Failure reason: {}", event.getReason());
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Cancel the order due to inventory unavailability
        if (order.getStatus() != OrderStatus.CANCELLED) {
            order.cancel();
            order = orderRepository.save(order);
            log.info("Order {} cancelled due to inventory reservation failure: {}", orderId, event.getReason());
        }
        
        return order;
    }

    private Order handleInventoryReleased(InventoryReleasedEvent event, UUID orderId) {
        log.info("Processing inventory released event for order: {}", orderId);
        log.info("Release reason: {}", event.getReason());
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Order is already cancelled, just log the inventory release
        log.info("Inventory released for order {}: {}", orderId, event.getReason());
        
        return order;
    }

}