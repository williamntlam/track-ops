package com.trackops.server.adapters.input.messaging.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.domain.model.outbox.InventoryReserveOutboxEntry;
import com.trackops.server.ports.output.persistence.outbox.InventoryReserveOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Consumes Debezium CDC events for inventory_reserve_outbox. For each insert (op=c),
 * produces to INVENTORY_RESERVE_REQUEST and marks the outbox row as SENT.
 * So: app enqueues to outbox → Debezium streams to Kafka → this relay produces to inventory topic and updates DB.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.inventory.reserve-request.mode", havingValue = "cdc")
public class InventoryReserveOutboxCdcRelayConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<UUID, String> outboxKafkaTemplate;
    private final InventoryReserveOutboxRepository outboxRepository;

    @Value("${app.inventory.reserve-request.topic:INVENTORY_RESERVE_REQUEST}")
    private String targetTopic;

    @Value("${app.inventory.reserve-request.send-timeout-seconds:10}")
    private int sendTimeoutSeconds;

    @KafkaListener(
        topics = "${app.inventory.reserve-request.cdc.topic:trackops_orders.public.inventory_reserve_outbox}",
        groupId = "trackops-orders-inventory-reserve-relay",
        containerFactory = "cdcStringListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void handleCdcEvent(String payload, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            // Debezium with ExtractNewRecordState: value is the flat "after" (no envelope)
            // Without ExtractNewRecordState: root has "after", "before", "source", "op"
            JsonNode row = root.has("after") ? root.get("after") : root;
            if (root.has("op") && !"c".equals(root.get("op").asText()) && !"r".equals(root.get("op").asText())) {
                log.debug("Ignoring CDC event op={}", root.get("op").asText());
                ack.acknowledge();
                return;
            }

            String orderIdStr = row.has("order_id") ? row.get("order_id").asText() : null;
            if (orderIdStr == null || orderIdStr.isBlank()) {
                log.warn("CDC outbox event missing order_id");
                ack.acknowledge();
                return;
            }

            UUID orderId = UUID.fromString(orderIdStr.trim());
            String body = objectMapper.writeValueAsString(Map.of("orderId", orderId.toString()));

            SendResult<UUID, String> result = outboxKafkaTemplate.send(targetTopic, orderId, body)
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);

            outboxRepository.findByOrderId(orderId)
                    .filter(e -> InventoryReserveOutboxEntry.STATUS_PENDING.equals(e.getStatus()))
                    .ifPresent(entry -> {
                        entry.markSent();
                        outboxRepository.save(entry);
                    });

            log.debug("Relayed inventory reserve request for order {} to {}", orderId, targetTopic);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to relay CDC outbox event: {}", e.getMessage(), e);
            throw e; // no ack, Kafka will redeliver
        }
    }
}
