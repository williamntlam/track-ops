package com.trackops.server.ports.output.inventory;

import java.util.UUID;

/**
 * Notifies the inventory service to reserve stock for an order.
 * Implementations should block until the request is acknowledged (e.g. Kafka produce ack)
 * so that the caller can ack the consuming message only after the inventory service has received the order.
 */
public interface InventoryReservationRequestPort {

    /**
     * Send a reserve-inventory request for the given order. Blocks until the request is delivered
     * (e.g. Kafka broker has acknowledged the produce). Throws if delivery fails.
     */
    void requestReservation(UUID orderId);
}
