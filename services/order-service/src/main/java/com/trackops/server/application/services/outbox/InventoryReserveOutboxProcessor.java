package com.trackops.server.application.services.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.domain.model.outbox.InventoryReserveOutboxEntry;
import com.trackops.server.ports.output.persistence.outbox.InventoryReserveOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Polls inventory_reserve_outbox for PENDING rows, sends each to Kafka, then marks SENT or increments retry / FAILED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.inventory.reserve-request.mode", havingValue = "outbox", matchIfMissing = true)
public class InventoryReserveOutboxProcessor {

    private final InventoryReserveOutboxRepository outboxRepository;
    private final KafkaTemplate<UUID, String> outboxKafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.inventory.reserve-request.topic:INVENTORY_RESERVE_REQUEST}")
    private String topic;

    @Value("${app.inventory.reserve-request.outbox.max-retries:5}")
    private int maxRetries;

    @Value("${app.inventory.reserve-request.outbox.batch-size:50}")
    private int batchSize;

    @Value("${app.inventory.reserve-request.send-timeout-seconds:10}")
    private int sendTimeoutSeconds;

    @Scheduled(fixedDelayString = "${app.inventory.reserve-request.outbox.poll-interval-ms:2000}")
    @Transactional
    public void processOutbox() {
        List<InventoryReserveOutboxEntry> pending = outboxRepository.findPending(maxRetries, batchSize);
        if (pending.isEmpty()) {
            return;
        }
        log.debug("Processing {} inventory reserve outbox entries", pending.size());
        for (InventoryReserveOutboxEntry entry : pending) {
            processEntry(entry);
        }
    }

    protected void processEntry(InventoryReserveOutboxEntry entry) {
        UUID orderId = entry.getOrderId();
        try {
            String payload = payload(orderId);
            SendResult<UUID, String> result = outboxKafkaTemplate.send(topic, orderId, payload)
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);
            entry.markSent();
            outboxRepository.save(entry);
            log.debug("Sent inventory reserve request for order {} to topic {}", orderId, topic);
        } catch (Exception e) {
            entry.incrementRetry(e.getMessage());
            if (entry.getRetryCount() >= maxRetries) {
                entry.markFailed(e.getMessage());
                log.warn("Inventory reserve outbox entry for order {} failed after {} retries: {}", orderId, maxRetries, e.getMessage());
            } else {
                log.debug("Inventory reserve send failed for order {}, retry {}/{}: {}", orderId, entry.getRetryCount(), maxRetries, e.getMessage());
            }
            outboxRepository.save(entry);
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
