package com.trackops.inventory.adapters.input.web.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInventoryItemRequest {
    
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String productName;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @Min(value = 0, message = "Available quantity must be non-negative")
    private Integer availableQuantity;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be positive")
    @Digits(integer = 8, fraction = 2, message = "Unit price must have at most 8 integer digits and 2 decimal places")
    private BigDecimal unitPrice;
    
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;
}
