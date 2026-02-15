package com.trackops.server.application.services.dlq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.domain.model.dlq.DlqOrder;
import com.trackops.server.ports.output.persistence.dlq.DlqOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for storing and querying failed order events in the PostgreSQL DLQ table.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DlqOrderService {

    private final DlqOrderRepository dlqOrderRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.dlq.max-retries:3}")
    private int defaultMaxRetries;

    private static final String ORDER_DEBEZIUM_TOPIC = "trackops_orders.public.orders";

    /**
     * Persist a failed order event to the DLQ table.
     * Call this from the Debezium error handler when the topic is the orders topic.
     */
    @Transactional
    public DlqOrder saveFailedOrderEvent(String topic, Object rawPayload, String messageType, Throwable cause) {
        String payloadJson = serializePayload(rawPayload);
        String orderId = extractOrderIdFromPayload(payloadJson);
        String errorLog = cause != null ? cause.getMessage() : "Unknown error";
        if (cause != null && cause.getCause() != null) {
            errorLog += "; " + cause.getCause().getMessage();
        }

        DlqOrder dlq = DlqOrder.builder()
                .orderId(orderId != null ? orderId : "unknown")
                .payload(payloadJson != null ? payloadJson : "{}")
                .messageType(messageType != null ? messageType : topic)
                .retryCount(0)
                .maxRetries(defaultMaxRetries)
                .status("PENDING")
                .errorLog(truncateErrorLog(errorLog))
                .createdAt(LocalDateTime.now())
                .nextRetryAt(LocalDateTime.now()) // eligible immediately for retry if desired
                .build();

        DlqOrder saved = dlqOrderRepository.save(dlq);
        log.warn("Saved failed order event to DLQ: id={}, orderId={}, topic={}", saved.getId(), orderId, topic);
        return saved;
    }

    public Optional<DlqOrder> findById(java.util.UUID id) {
        return dlqOrderRepository.findById(id);
    }

    public List<DlqOrder> findByOrderId(String orderId) {
        return dlqOrderRepository.findByOrderId(orderId);
    }

    public List<DlqOrder> findByStatus(String status) {
        return dlqOrderRepository.findByStatus(status);
    }

    public List<DlqOrder> findPendingEligibleForRetry() {
        return dlqOrderRepository.findPendingEligibleForRetry(LocalDateTime.now());
    }

    public long countPending() {
        return dlqOrderRepository.countPending();
    }

    public long countByStatus(String status) {
        return dlqOrderRepository.countByStatus(status);
    }

    /**
     * Whether this topic should be persisted to dlq_orders (order events only).
     */
    public boolean isOrderEventTopic(String topic) {
        return ORDER_DEBEZIUM_TOPIC.equals(topic);
    }

    private String serializePayload(Object rawPayload) {
        if (rawPayload == null) return "{}";
        if (rawPayload instanceof String) return (String) rawPayload;
        try {
            return objectMapper.writeValueAsString(rawPayload);
        } catch (Exception e) {
            log.debug("Could not serialize payload to JSON", e);
            return rawPayload.toString();
        }
    }

    private String extractOrderIdFromPayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isEmpty()) return null;
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            JsonNode payload = root.get("payload");
            if (payload == null) return null;
            JsonNode after = payload.get("after");
            if (after != null && after.has("id")) return after.get("id").asText();
            JsonNode before = payload.get("before");
            if (before != null && before.has("id")) return before.get("id").asText();
        } catch (Exception e) {
            log.trace("Could not extract order_id from payload", e);
        }
        return null;
    }

    private static String truncateErrorLog(String s) {
        if (s == null) return null;
        final int max = 10000;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
