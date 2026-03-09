package com.trackops.inventory.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    
    @Column(name = "product_id", nullable = false)
    private String productId;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;
    
    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Column(name = "released_at")
    private Instant releasedAt;
    
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
        if (this.status == null) {
            this.status = ReservationStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
    
    public void markAsReserved() {
        this.status = ReservationStatus.RESERVED;
        this.reservedAt = Instant.now();
    }
    
    public void markAsReleased() {
        this.status = ReservationStatus.RELEASED;
        this.releasedAt = Instant.now();
    }
    
    public void markAsExpired() {
        this.status = ReservationStatus.EXPIRED;
    }
    
    public boolean isExpired() {
        return this.expiresAt != null && Instant.now().isAfter(this.expiresAt);
    }
    
    public boolean isActive() {
        return this.status == ReservationStatus.RESERVED && !isExpired();
    }
}
