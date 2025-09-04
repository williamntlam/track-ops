package com.trackops.server.application.services.events;

import org.springframework.stereotype.Service;
import com.trackops.server.ports.input.events.OrderEventProcessorPort;
import com.trackops.server.domain.events.orders.OrderEvent;
import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import com.trackops.server.ports.output.persistence.events.ProcessedEventRepository;
import com.trackops.server.ports.output.cache.IdempotencyCachePort; 
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

            // Step Two: Load and Process the order
            Order processedOrder = orderRepository.findById(orderId)
                .map(order -> {
                    order.process();
                    return orderRepository.save(order);
                })
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            // Step Three: Mark event as processed
            ProcessedEvent processedEvent = ProcessedEvent.createForOrderEvent(
                eventId,
                orderId,
                event.getEventType(),
                processedOrder.getStatus(), 
                "order-processor",
                0L
            );
            processedEventRepository.save(processedEvent);

            // Step Four: Mark as processed in idempotency cache
            idempotencyCachePort.markEventProcessed(eventId, Duration.ofHours(24));

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

}