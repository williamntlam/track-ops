package com.trackops.inventory.adapters.input.web.dto;

import com.trackops.inventory.domain.model.InventoryReservation;
import com.trackops.inventory.domain.model.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationResponse {
    
    private UUID id;
    private UUID orderId;
    private String productId;
    private String productName;
    private Integer quantity;
    private ReservationStatus status;
    private Instant reservedAt;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
    
    public static InventoryReservationResponse from(InventoryReservation reservation) {
        return InventoryReservationResponse.builder()
            .id(reservation.getId())
            .orderId(reservation.getOrderId())
            .productId(reservation.getProductId())
            .productName("N/A") // Will be populated from join if needed
            .quantity(reservation.getQuantity())
            .status(reservation.getStatus())
            .reservedAt(reservation.getReservedAt())
            .expiresAt(reservation.getExpiresAt())
            .createdAt(reservation.getCreatedAt())
            .updatedAt(reservation.getUpdatedAt())
            .build();
    }
}
