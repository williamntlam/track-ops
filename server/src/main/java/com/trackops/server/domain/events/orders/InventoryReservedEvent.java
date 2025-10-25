package com.trackops.server.domain.events.orders;

import com.trackops.server.domain.events.orders.OrderEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryReservedEvent extends OrderEvent {
    
    private final String reservationId;
    private final List<ReservedItem> reservedItems;
    
    public InventoryReservedEvent(UUID orderId, String reservationId, List<ReservedItem> reservedItems) {
        super(orderId, "INVENTORY_RESERVED");
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
