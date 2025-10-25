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
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .version(item.getVersion())
            .build();
    }
}
