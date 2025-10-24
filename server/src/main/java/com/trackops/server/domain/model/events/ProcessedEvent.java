package com.trackops.server.domain.model.events;

import com.trackops.server.domain.model.enums.EventType;
import com.trackops.server.domain.model.enums.OrderStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private String eventId;
    
    @Column(name = "order_id")
    private Long orderId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType eventType;
    
    @Column(name = "processed_at")
    private Instant processedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus;
    
    @Column(name = "consumer_group")
    private String consumerGroup;
    
    @Column(name = "offset_value")
    private Long offset;

    // Default constructor for JPA
    public ProcessedEvent() {}

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

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    
    public OrderStatus getOrderStatus() { return orderStatus; }
    public void setOrderStatus(OrderStatus orderStatus) { this.orderStatus = orderStatus; }
    
    public String getConsumerGroup() { return consumerGroup; }
    public void setConsumerGroup(String consumerGroup) { this.consumerGroup = consumerGroup; }
    
    public Long getOffset() { return offset; }
    public void setOffset(Long offset) { this.offset = offset; }
}