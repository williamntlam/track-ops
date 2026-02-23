package com.trackops.server.adapters.output.messaging.inventory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.ports.output.inventory.InventoryReservationRequestPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Sends reserve-inventory requests to Kafka. Blocks until the broker acknowledges the produce
 * so that the order-event consumer can ack only after the inventory service has received the order.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.inventory.reserve-request.enabled", havingValue = "true")
@ConditionalOnProperty(name = "app.inventory.reserve-request.mode", havingValue = "sync")
public class KafkaInventoryReservationRequestAdapter implements InventoryReservationRequestPort {

    private final KafkaTemplate<UUID, String> outboxKafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.inventory.reserve-request.topic:INVENTORY_RESERVE_REQUEST}")
    private String topic;

    @Value("${app.inventory.reserve-request.send-timeout-seconds:10}")
    private int sendTimeoutSeconds;

    @Override
    public void requestReservation(UUID orderId) {
        String payload = payload(orderId);
        try {
            SendResult<UUID, String> result = outboxKafkaTemplate.send(topic, orderId, payload)
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);
            log.debug("Inventory reserve request sent for order {} to topic {}", orderId, topic);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while sending inventory reserve request for order " + orderId, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to send inventory reserve request for order " + orderId + ": " + e.getCause().getMessage(), e.getCause());
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout sending inventory reserve request for order " + orderId, e);
        }
    }

    private String payload(UUID orderId) {
        try {
            return objectMapper.writeValueAsString(Map.of("orderId", orderId.toString()));
        } catch (JsonProcessingException e) {
            return "{\"orderId\":\"" + orderId + "\"}";
        }
    }
}
