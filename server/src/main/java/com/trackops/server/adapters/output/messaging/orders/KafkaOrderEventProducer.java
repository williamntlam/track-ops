package com.trackops.server.adapters.output.messaging.orders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.domain.events.orders.OrderCancelledEvent;
import com.trackops.server.domain.events.orders.OrderCreatedEvent;
import com.trackops.server.domain.events.orders.OrderDeliveredEvent;
import com.trackops.server.domain.events.orders.OrderEvent;
import com.trackops.server.domain.events.orders.OrderStatusUpdatedEvent;
import com.trackops.server.ports.output.events.orders.OrderEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@Service
public class KafkaOrderEventProducer implements OrderEventProducer {

    private final KafkaTemplate<UUID, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaOrderEventProducer(KafkaTemplate<UUID, String> kafkaTemplate, ObjectMapper objectMapper) {

        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;

    }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        
        publishEvent(event);

    }

    @Override
    public void publishOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        
        publishEvent(event);

    }

    @Override
    public void publishOrderDelivered(OrderDeliveredEvent event) {
        
        publishEvent(event);

    }

    @Override
    public void publishOrderCancelled(OrderCancelledEvent event) {
        
        publishEvent(event);

    }

    private void publishEvent(OrderEvent event) {

        try {

            String topic = event.getEventType();
            UUID key = event.getOrderId();
            String value = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(topic, key, value);

        } catch (JsonProcessingException err) {

            log.error("Failed to serialize event for order {}: {}", 
                    event.getOrderId(), err.getMessage(), err);
            throw new RuntimeException("Failed to serialize event", err);

        } 

    }

}

