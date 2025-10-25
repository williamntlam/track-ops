package com.trackops.server.application.services.events;

import org.springframework.stereotype.Service;
import com.trackops.server.ports.input.events.OrderEventProcessorPort;
import com.trackops.server.domain.events.orders.OrderEvent;
import com.trackops.server.domain.events.orders.InventoryReservedEvent;
import com.trackops.server.domain.events.orders.InventoryReservationFailedEvent;
import com.trackops.server.domain.events.orders.InventoryReleasedEvent;
import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import com.trackops.server.ports.output.persistence.events.ProcessedEventRepository;
import com.trackops.server.ports.output.cache.IdempotencyCachePort; 
import com.trackops.server.domain.model.CacheOperationResult;
import com.trackops.server.domain.model.events.ProcessedEvent;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.enums.EventType;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderEventProcessorService implements OrderEventProcessorPort {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProcessorService.class);

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final IdempotencyCachePort idempotencyCachePort;

    public OrderEventProcessorService(OrderRepository orderRepository, ProcessedEventRepository processedEventRepository, IdempotencyCachePort idempotencyCachePort) {
        
        this.orderRepository = orderRepository;
        this.processedEventRepository = processedEventRepository;
        this.idempotencyCachePort = idempotencyCachePort;

    }

    public void processOrderEvent(OrderEvent event) {
        
        try {
            // Validate input event
            validateEvent(event);
            
            UUID eventId = event.getEventId();
            UUID orderId = event.getOrderId();

            // Step One: Check idempotency
            if (idempotencyCachePort.isEventProcessed(eventId)) {
                return; // Already processed
            }

            // Step Two: Handle different event types
            Order processedOrder = handleEventByType(event, orderId);

            // Step Three: Mark event as processed
            ProcessedEvent processedEvent = ProcessedEvent.createForOrderEvent(
                eventId,
                orderId,
                EventType.valueOf(event.getEventType()),
                processedOrder.getStatus(), 
                "order-processor",
                0L
            );
            processedEventRepository.save(processedEvent);

            // Step Four: Mark as processed in idempotency cache
            CacheOperationResult cacheResult = idempotencyCachePort.markEventProcessed(eventId, Duration.ofHours(24));
            if (cacheResult.isFailure()) {
                log.warn("Failed to mark event {} as processed in cache: {}", eventId, cacheResult.getErrorMessage());
                // Don't fail the entire operation, just log the warning
            }

        } catch (Exception e) {
            log.error("Failed to process order event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to process order event", e);
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
            order.cancel("Inventory reservation failed: " + event.getReason());
            order = orderRepository.save(order);
            log.info("Order {} cancelled due to inventory reservation failure", orderId);
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