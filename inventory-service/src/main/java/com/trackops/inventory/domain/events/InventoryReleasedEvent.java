package com.trackops.inventory.domain.events;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryReleasedEvent extends InventoryEvent {
    
    private final String reservationId;
    private final List<ReleasedItem> releasedItems;
    private final String reason;
    
    public InventoryReleasedEvent(UUID orderId, String reservationId, List<ReleasedItem> releasedItems, String reason) {
        super("INVENTORY_RELEASED", orderId);
        this.reservationId = reservationId;
        this.releasedItems = releasedItems;
        this.reason = reason;
    }
    
    @Data
    public static class ReleasedItem {
        private final String productId;
        private final String productName;
        private final Integer quantity;
        
        public ReleasedItem(String productId, String productName, Integer quantity) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
        }
    }
}
