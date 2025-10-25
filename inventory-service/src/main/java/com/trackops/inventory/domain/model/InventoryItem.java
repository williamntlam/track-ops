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
}
