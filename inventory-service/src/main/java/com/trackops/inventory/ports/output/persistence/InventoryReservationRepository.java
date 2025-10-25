package com.trackops.inventory.ports.output.persistence;

import com.trackops.inventory.domain.model.InventoryReservation;
import com.trackops.inventory.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    
    List<InventoryReservation> findByOrderId(UUID orderId);
    
    List<InventoryReservation> findByStatus(ReservationStatus status);
    
    @Query("SELECT r FROM InventoryReservation r WHERE r.orderId = :orderId AND r.status = :status")
    List<InventoryReservation> findByOrderIdAndStatus(@Param("orderId") UUID orderId, @Param("status") ReservationStatus status);
    
    @Query("SELECT r FROM InventoryReservation r WHERE r.expiresAt < :now AND r.status = 'RESERVED'")
    List<InventoryReservation> findExpiredReservations(@Param("now") Instant now);
    
    @Query("SELECT r FROM InventoryReservation r WHERE r.productId = :productId AND r.status = 'RESERVED'")
    List<InventoryReservation> findActiveReservationsByProductId(@Param("productId") String productId);
    
    Optional<InventoryReservation> findByOrderIdAndProductId(UUID orderId, String productId);
    
    @Query("SELECT COUNT(r) FROM InventoryReservation r WHERE r.orderId = :orderId AND r.status = 'RESERVED'")
    long countActiveReservationsByOrderId(@Param("orderId") UUID orderId);
}
