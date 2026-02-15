package com.trackops.server.adapters.input.messaging;

import com.trackops.server.application.services.dlq.DlqOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Error handler for Debezium consumers. Persists failed order events to the PostgreSQL dlq_orders table.
 */
@Slf4j
@Component("debeziumErrorHandler")
@RequiredArgsConstructor
public class DlqOrderErrorHandler implements KafkaListenerErrorHandler {

    private final DlqOrderService dlqOrderService;

    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException exception) {
        log.error("Debezium consumer error occurred", exception);

        if (message == null) {
            return null;
        }

        String topic = getHeader(message, KafkaHeaders.RECEIVED_TOPIC);
        Object payload = message.getPayload();

        log.error("Failed message topic: {}, payload: {}", topic, payload);

        if (dlqOrderService.isOrderEventTopic(topic)) {
            try {
                dlqOrderService.saveFailedOrderEvent(
                        topic,
                        payload,
                        "debezium-order-event",
                        exception
                );
            } catch (Exception e) {
                log.error("Failed to persist order event to DLQ table", e);
            }
        }

        return null;
    }

    private static String getHeader(Message<?> message, String key) {
        return Optional.ofNullable(message.getHeaders().get(key, String.class)).orElse("");
    }
}
