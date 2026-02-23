package com.trackops.server.adapters.output.messaging.inventory;

import com.trackops.server.ports.output.inventory.InventoryReservationRequestPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * No-op implementation when inventory reserve-request notification is disabled.
 * Allows the app to run without an inventory service / topic.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.inventory.reserve-request.enabled", havingValue = "false")
public class NoOpInventoryReservationRequestAdapter implements InventoryReservationRequestPort {

    @Override
    public void requestReservation(UUID orderId) {
        log.debug("Inventory reserve request disabled; skipping for order {}", orderId);
    }
}
