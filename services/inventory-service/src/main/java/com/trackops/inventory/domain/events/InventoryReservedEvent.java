package com.trackops.inventory.domain.events;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryReservedEvent extends InventoryEvent {
    
    private final List<ReservedItem> reservedItems;
    private final String reservationId;
    
    public InventoryReservedEvent(UUID orderId, String reservationId, List<ReservedItem> reservedItems) {
        super("INVENTORY_RESERVED", orderId);
        this.reservationId = reservationId;
        this.reservedItems = reservedItems;
    }
    
    @Data
    public static class ReservedItem {
        private final String productId;
        private final String productName;
        private final Integer quantity;
        private final String unitPrice;
        
        public ReservedItem(String productId, String productName, Integer quantity, String unitPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
    }
}
