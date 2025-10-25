package com.trackops.server.domain.events.orders;

import com.trackops.server.domain.events.orders.OrderEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryReleasedEvent extends OrderEvent {
    
    private final String reservationId;
    private final List<ReleasedItem> releasedItems;
    private final String reason;
    
    public InventoryReleasedEvent(UUID orderId, String reservationId, List<ReleasedItem> releasedItems, String reason) {
        super(orderId, "INVENTORY_RELEASED");
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
