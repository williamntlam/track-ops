package com.trackops.inventory.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "product_id", nullable = false, unique = true)
    private String productId;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;
    
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "min_stock_level")
    private Integer minStockLevel;
    
    @Column(name = "max_stock_level")
    private Integer maxStockLevel;
    
    @Column(name = "reorder_quantity")
    private Integer reorderQuantity;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "is_discontinued", nullable = false)
    private Boolean isDiscontinued = false;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    private Long version;
    
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.reservedQuantity == null) {
            this.reservedQuantity = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
    
    public boolean hasAvailableQuantity(int requestedQuantity) {
        return this.availableQuantity >= requestedQuantity;
    }
    
    public void reserveQuantity(int quantity) {
        if (!hasAvailableQuantity(quantity)) {
            throw new IllegalArgumentException("Insufficient inventory for product: " + productId);
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }
    
    public void releaseQuantity(int quantity) {
        if (this.reservedQuantity < quantity) {
            throw new IllegalArgumentException("Cannot release more than reserved quantity for product: " + productId);
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }
    
    public int getTotalQuantity() {
        return this.availableQuantity + this.reservedQuantity;
    }
    
    // Business logic methods
    public boolean isAvailableForReservation() {
        return this.isActive && !this.isDiscontinued;
    }
    
    public boolean isLowStock() {
        return this.minStockLevel != null && this.availableQuantity <= this.minStockLevel;
    }
    
    public boolean isOutOfStock() {
        return this.availableQuantity <= 0;
    }
    
    public boolean needsReordering() {
        return this.isLowStock() && this.reorderQuantity != null && this.reorderQuantity > 0;
    }
    
    public void adjustStock(int adjustment) {
        if (this.availableQuantity + adjustment < 0) {
            throw new IllegalArgumentException("Stock adjustment would result in negative available quantity");
        }
        this.availableQuantity += adjustment;
    }
    
    public void setMinStockLevel(Integer minStockLevel) {
        if (minStockLevel != null && minStockLevel < 0) {
            throw new IllegalArgumentException("Minimum stock level cannot be negative");
        }
        this.minStockLevel = minStockLevel;
    }
    
    public void setMaxStockLevel(Integer maxStockLevel) {
        if (maxStockLevel != null && maxStockLevel < 0) {
            throw new IllegalArgumentException("Maximum stock level cannot be negative");
        }
        if (maxStockLevel != null && this.minStockLevel != null && maxStockLevel < this.minStockLevel) {
            throw new IllegalArgumentException("Maximum stock level cannot be less than minimum stock level");
        }
        this.maxStockLevel = maxStockLevel;
    }
    
    public void discontinue() {
        this.isDiscontinued = true;
        this.isActive = false;
    }
    
    public void reactivate() {
        this.isDiscontinued = false;
        this.isActive = true;
    }
}
