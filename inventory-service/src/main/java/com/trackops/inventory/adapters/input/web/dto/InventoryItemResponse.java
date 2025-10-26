package com.trackops.inventory.adapters.input.web.dto;

import com.trackops.inventory.domain.model.InventoryItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemResponse {
    
    private UUID id;
    private String productId;
    private String productName;
    private String description;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private BigDecimal unitPrice;
    private String category;
    private Integer minStockLevel;
    private Integer maxStockLevel;
    private Integer reorderQuantity;
    private Boolean isActive;
    private Boolean isDiscontinued;
    private Boolean isLowStock;
    private Boolean isOutOfStock;
    private Boolean needsReordering;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
    
    public static InventoryItemResponse from(InventoryItem item) {
        return InventoryItemResponse.builder()
            .id(item.getId())
            .productId(item.getProductId())
            .productName(item.getProductName())
            .description(item.getDescription())
            .availableQuantity(item.getAvailableQuantity())
            .reservedQuantity(item.getReservedQuantity())
            .unitPrice(item.getUnitPrice())
            .category(item.getCategory())
            .minStockLevel(item.getMinStockLevel())
            .maxStockLevel(item.getMaxStockLevel())
            .reorderQuantity(item.getReorderQuantity())
            .isActive(item.getIsActive())
            .isDiscontinued(item.getIsDiscontinued())
            .isLowStock(item.isLowStock())
            .isOutOfStock(item.isOutOfStock())
            .needsReordering(item.needsReordering())
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .version(item.getVersion())
            .build();
    }
}
