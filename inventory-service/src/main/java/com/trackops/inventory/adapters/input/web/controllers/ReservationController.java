package com.trackops.inventory.adapters.input.web.controllers;

import com.trackops.inventory.adapters.input.web.dto.InventoryReservationResponse;
import com.trackops.inventory.adapters.input.web.dto.ReservationStatsResponse;
import com.trackops.inventory.domain.model.InventoryReservation;
import com.trackops.inventory.domain.model.ReservationStatus;
import com.trackops.inventory.ports.output.persistence.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/inventory/reservations")
@RequiredArgsConstructor
public class ReservationController {
    
    private final InventoryReservationRepository reservationRepository;
    
    /**
     * Get all reservations with pagination
     */
    @GetMapping
    public ResponseEntity<Page<InventoryReservationResponse>> getAllReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) ReservationStatus status) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<InventoryReservation> reservations;
            if (status != null) {
                reservations = reservationRepository.findByStatus(status, pageable);
            } else {
                reservations = reservationRepository.findAll(pageable);
            }
            
            Page<InventoryReservationResponse> response = reservations.map(InventoryReservationResponse::from);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving reservations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get reservations by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<InventoryReservationResponse>> getReservationsByOrderId(@PathVariable UUID orderId) {
        try {
            List<InventoryReservation> reservations = reservationRepository.findByOrderId(orderId);
            List<InventoryReservationResponse> response = reservations.stream()
                .map(InventoryReservationResponse::from)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving reservations for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get reservations by order ID and status
     */
    @GetMapping("/order/{orderId}/status/{status}")
    public ResponseEntity<List<InventoryReservationResponse>> getReservationsByOrderIdAndStatus(
            @PathVariable UUID orderId,
            @PathVariable ReservationStatus status) {
        
        try {
            List<InventoryReservation> reservations = reservationRepository.findByOrderIdAndStatus(orderId, status);
            List<InventoryReservationResponse> response = reservations.stream()
                .map(InventoryReservationResponse::from)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving reservations for order {} with status {}: {}", orderId, status, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get active reservations for a product
     */
    @GetMapping("/product/{productId}/active")
    public ResponseEntity<List<InventoryReservationResponse>> getActiveReservationsByProductId(@PathVariable String productId) {
        try {
            List<InventoryReservation> reservations = reservationRepository.findActiveReservationsByProductId(productId);
            List<InventoryReservationResponse> response = reservations.stream()
                .map(InventoryReservationResponse::from)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving active reservations for product {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get specific reservation by ID
     */
    @GetMapping("/{reservationId}")
    public ResponseEntity<InventoryReservationResponse> getReservationById(@PathVariable UUID reservationId) {
        try {
            return reservationRepository.findById(reservationId)
                .map(InventoryReservationResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            log.error("Error retrieving reservation {}: {}", reservationId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get expired reservations
     */
    @GetMapping("/expired")
    public ResponseEntity<List<InventoryReservationResponse>> getExpiredReservations() {
        try {
            List<InventoryReservation> reservations = reservationRepository.findExpiredReservations(Instant.now());
            List<InventoryReservationResponse> response = reservations.stream()
                .map(InventoryReservationResponse::from)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving expired reservations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get reservation statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ReservationStatsResponse> getReservationStats() {
        try {
            List<InventoryReservation> allReservations = reservationRepository.findAll();
            
            long totalReservations = allReservations.size();
            long activeReservations = allReservations.stream()
                .mapToLong(r -> r.getStatus() == ReservationStatus.RESERVED ? 1 : 0)
                .sum();
            long expiredReservations = allReservations.stream()
                .mapToLong(r -> r.getStatus() == ReservationStatus.EXPIRED ? 1 : 0)
                .sum();
            long releasedReservations = allReservations.stream()
                .mapToLong(r -> r.getStatus() == ReservationStatus.RELEASED ? 1 : 0)
                .sum();
            
            ReservationStatsResponse stats = ReservationStatsResponse.builder()
                .totalReservations(totalReservations)
                .activeReservations(activeReservations)
                .expiredReservations(expiredReservations)
                .releasedReservations(releasedReservations)
                .build();
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error retrieving reservation statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
