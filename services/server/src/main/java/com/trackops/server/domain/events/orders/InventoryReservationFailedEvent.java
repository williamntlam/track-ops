package com.trackops.server.domain.events.orders;

import com.trackops.server.domain.events.orders.OrderEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryReservationFailedEvent extends OrderEvent {
    
    private final String reason;
    private final List<FailedItem> failedItems;
    
    public InventoryReservationFailedEvent(UUID orderId, String reason, List<FailedItem> failedItems) {
        super(orderId, "INVENTORY_RESERVATION_FAILED");
        this.reason = reason;
        this.failedItems = failedItems;
    }
    
    @Data
    public static class FailedItem {
        private final String productId;
        private final String productName;
        private final Integer requestedQuantity;
        private final Integer availableQuantity;
        private final String failureReason;
        
        public FailedItem(String productId, String productName, Integer requestedQuantity, 
                         Integer availableQuantity, String failureReason) {
            this.productId = productId;
            this.productName = productName;
            this.requestedQuantity = requestedQuantity;
            this.availableQuantity = availableQuantity;
            this.failureReason = failureReason;
        }
    }
}
