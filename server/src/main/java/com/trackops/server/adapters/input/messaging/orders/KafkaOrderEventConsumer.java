package com.trackops.server.adapters.input.messaging.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.ports.input.events.OrderEventProcessorPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class KafkaOrderEventConsumer {

    private final OrderEventProcessorPort orderEventProcessor;
    private final ObjectMapper objectMapper;

    public KafkaOrderEventConsumer(OrderEventProcessorPort orderEventProcessor, ObjectMapper objectMapper) {
        this.orderEventProcessor = orderEventProcessor;
        this.objectMapper = objectMapper;
    }

    // Your @KafkaListener methods will go here
    // Step 2: Add ORDER_CREATED listener
    // Step 3: Add ORDER_STATUS_UPDATED listener  
    // Step 4: Add ORDER_DELIVERED listener
    // Step 5: Add ORDER_CANCELLED listener
}