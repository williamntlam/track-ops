package com.trackops.server.adapters.input.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Error handler for Debezium consumers.
 * Handles errors gracefully and logs them for monitoring.
 * Only active when Debezium strategy is enabled.
 */
@Slf4j
@Component("debeziumErrorHandler")
@ConditionalOnProperty(name = "app.event-publishing.strategy", havingValue = "debezium")
public class DebeziumErrorHandler implements KafkaListenerErrorHandler {
    
    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException exception) {
        log.error("Debezium consumer error occurred", exception);
        
        // Log the failed message details
        if (message != null) {
            log.error("Failed message headers: {}", message.getHeaders());
            log.error("Failed message payload: {}", message.getPayload());
        }
        
        // For now, we'll just log the error and continue
        // In production, you might want to:
        // 1. Send to dead letter queue
        // 2. Retry with exponential backoff
        // 3. Alert monitoring systems
        
        return null;
    }
}
