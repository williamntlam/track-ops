package com.trackops.server.adapters.output.messaging.inventory;

import com.trackops.server.ports.output.inventory.InventoryReservationRequestPort;
import com.trackops.server.ports.output.persistence.outbox.InventoryReserveOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Enqueues an inventory reserve request into the outbox (same transaction as order processing).
 * A separate processor sends from the outbox to Kafka with retries and marks SENT/FAILED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${app.inventory.reserve-request.enabled:true}' == 'true' && '${app.inventory.reserve-request.mode:outbox}' != 'sync'")
public class OutboxInventoryReservationRequestAdapter implements InventoryReservationRequestPort {

    private final InventoryReserveOutboxRepository outboxRepository;

    @Override
    public void requestReservation(UUID orderId) {
        boolean enqueued = outboxRepository.enqueueIfAbsent(orderId);
        if (enqueued) {
            log.debug("Enqueued inventory reserve request for order {}", orderId);
        } else {
            log.debug("Inventory reserve request for order {} already enqueued (idempotent)", orderId);
        }
    }
}
