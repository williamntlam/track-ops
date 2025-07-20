package com.trackops.server.domain.model.events;

import com.trackops.server.domain.model.enums.EventType;
import com.trackops.server.domain.model.enums.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public class ProcessedEvent {

    private final String eventId;
    private final Long orderId;
    private final EventType eventType;
    private final Instant processedAt;
    private final OrderStatus orderStatus;
    private final String consumerGroup;
    private final Long offset;

    public ProcessedEvent(String eventId, Long orderId, EventType eventType, Instant processedAt, OrderStatus orderStatus, String consumerGroup, Long offset) {

        this.eventId = eventId;
        this.orderId = orderId;
        this.eventType = eventType;
        this.processedAt = processedAt;
        this.orderStatus = orderStatus;
        this.consumerGroup = consumerGroup;
        this.offset = offset;

    }

    public static ProcessedEvent createForOrderEvent(
            UUID eventId, 
            UUID orderId, 
            EventType eventType, 
            OrderStatus orderStatus, 
            String consumerGroup, 
            Long offset) {
        
        return new ProcessedEvent(
            eventId.toString(),
            orderId.getMostSignificantBits(), // Convert UUID to Long
            eventType,
            Instant.now(),
            orderStatus,
            consumerGroup,
            offset
        );
    }

    public String getEventId() { return eventId; }
    public Long getOrderId() { return orderId; }
    public EventType getEventType() { return eventType; }
    public Instant getProcessedAt() { return processedAt; }
    public OrderStatus getOrderStatus() { return orderStatus; }
    public String getConsumerGroup() { return consumerGroup; }
    public Long getOffset() { return offset; }

}