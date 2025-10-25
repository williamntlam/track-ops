package com.trackops.inventory.ports.output.events;

import com.trackops.inventory.domain.events.InventoryReservedEvent;
import com.trackops.inventory.domain.events.InventoryReservationFailedEvent;
import com.trackops.inventory.domain.events.InventoryReleasedEvent;

public interface InventoryEventProducer {
    void publishInventoryReserved(InventoryReservedEvent event);
    void publishInventoryReservationFailed(InventoryReservationFailedEvent event);
    void publishInventoryReleased(InventoryReleasedEvent event);
}
