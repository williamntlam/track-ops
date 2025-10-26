package com.trackops.inventory.application.services;

import com.trackops.inventory.domain.events.InventoryReservedEvent;
import com.trackops.inventory.domain.events.InventoryReservationFailedEvent;
import com.trackops.inventory.domain.events.InventoryReleasedEvent;
import com.trackops.inventory.domain.model.InventoryItem;
import com.trackops.inventory.domain.model.InventoryReservation;
import com.trackops.inventory.domain.model.ReservationStatus;
import com.trackops.inventory.ports.input.events.OrderEventProcessorPort;
import com.trackops.inventory.ports.output.events.InventoryEventProducer;
import com.trackops.inventory.ports.output.persistence.InventoryItemRepository;
import com.trackops.inventory.ports.output.persistence.InventoryReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class InventoryService implements OrderEventProcessorPort {
    
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryEventProducer eventProducer;
    
    public InventoryService(InventoryItemRepository inventoryItemRepository,
                          InventoryReservationRepository reservationRepository,
                          InventoryEventProducer eventProducer) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.reservationRepository = reservationRepository;
        this.eventProducer = eventProducer;
    }
    
    @Override
    @Transactional
    public void processOrderEvent(com.trackops.inventory.domain.events.OrderEvent event) {
        log.info("Processing order event: {} for order: {}", event.getEventType(), event.getOrderId());
        
        switch (event.getEventType()) {
            case "ORDER_CREATED":
                handleOrderCreated(event);
                break;
            case "ORDER_CANCELLED":
                handleOrderCancelled(event);
                break;
            default:
                log.debug("No inventory processing needed for event type: {}", event.getEventType());
        }
    }
    
    @Transactional
    public void handleOrderCreated(com.trackops.inventory.domain.events.OrderEvent event) {
        try {
            UUID orderId = event.getOrderId();
            log.info("Handling order created event for order: {}", orderId);
            
            // For demo purposes, we'll create a simple order with some default items
            // In a real system, the order would contain the actual items
            List<ReservationRequest> reservationRequests = createDefaultReservationRequests();
            
            List<InventoryReservation> reservations = new ArrayList<>();
            List<InventoryReservationFailedEvent.FailedItem> failedItems = new ArrayList<>();
            
            // Try to reserve inventory for each item
            for (ReservationRequest request : reservationRequests) {
                try {
                    InventoryItem item = inventoryItemRepository.findByProductId(request.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));
                    
                    // Check business rules before reservation
                    if (!item.isAvailableForReservation()) {
                        failedItems.add(new InventoryReservationFailedEvent.FailedItem(
                            request.getProductId(),
                            item.getProductName(),
                            request.getQuantity(),
                            item.getAvailableQuantity(),
                            item.getIsDiscontinued() ? "Product is discontinued" : "Product is inactive"
                        ));
                        continue;
                    }
                    
                    if (item.hasAvailableQuantity(request.getQuantity())) {
                        // Reserve the inventory
                        item.reserveQuantity(request.getQuantity());
                        inventoryItemRepository.save(item);
                        
                        // Create reservation record
                        InventoryReservation reservation = InventoryReservation.builder()
                            .orderId(orderId)
                            .productId(request.getProductId())
                            .quantity(request.getQuantity())
                            .status(ReservationStatus.RESERVED)
                            .reservedAt(Instant.now())
                            .expiresAt(Instant.now().plusSeconds(1800)) // 30 minutes
                            .build();
                        
                        reservations.add(reservationRepository.save(reservation));
                        
                        log.info("Successfully reserved {} units of product {} for order {}", 
                                request.getQuantity(), request.getProductId(), orderId);
                    } else {
                        failedItems.add(new InventoryReservationFailedEvent.FailedItem(
                            request.getProductId(),
                            item.getProductName(),
                            request.getQuantity(),
                            item.getAvailableQuantity(),
                            "Insufficient inventory"
                        ));
                    }
                } catch (Exception e) {
                    log.error("Failed to reserve inventory for product {}: {}", 
                            request.getProductId(), e.getMessage());
                    failedItems.add(new InventoryReservationFailedEvent.FailedItem(
                        request.getProductId(),
                        "Unknown Product",
                        request.getQuantity(),
                        0,
                        e.getMessage()
                    ));
                }
            }
            
            // Publish appropriate event based on results
            if (failedItems.isEmpty()) {
                // All reservations successful
                String reservationId = UUID.randomUUID().toString();
                List<InventoryReservedEvent.ReservedItem> reservedItems = reservations.stream()
                    .map(r -> new InventoryReservedEvent.ReservedItem(
                        r.getProductId(),
                        inventoryItemRepository.findByProductId(r.getProductId())
                            .map(InventoryItem::getProductName)
                            .orElse("Unknown"),
                        r.getQuantity(),
                        inventoryItemRepository.findByProductId(r.getProductId())
                            .map(item -> item.getUnitPrice().toString())
                            .orElse("0.00")
                    ))
                    .toList();
                
                InventoryReservedEvent reservedEvent = new InventoryReservedEvent(orderId, reservationId, reservedItems);
                eventProducer.publishInventoryReserved(reservedEvent);
                
                log.info("Successfully reserved inventory for order: {}", orderId);
            } else {
                // Some or all reservations failed
                InventoryReservationFailedEvent failedEvent = new InventoryReservationFailedEvent(
                    orderId, 
                    "Inventory reservation failed", 
                    failedItems
                );
                eventProducer.publishInventoryReservationFailed(failedEvent);
                
                log.warn("Inventory reservation failed for order: {} with {} failed items", 
                        orderId, failedItems.size());
            }
            
        } catch (Exception e) {
            log.error("Error processing order created event for order {}: {}", 
                    event.getOrderId(), e.getMessage(), e);
            
            // Publish failure event
            InventoryReservationFailedEvent failedEvent = new InventoryReservationFailedEvent(
                event.getOrderId(),
                "System error: " + e.getMessage(),
                List.of()
            );
            eventProducer.publishInventoryReservationFailed(failedEvent);
        }
    }
    
    @Transactional
    public void handleOrderCancelled(com.trackops.inventory.domain.events.OrderEvent event) {
        try {
            UUID orderId = event.getOrderId();
            log.info("Handling order cancelled event for order: {}", orderId);
            
            List<InventoryReservation> reservations = reservationRepository.findByOrderIdAndStatus(
                orderId, ReservationStatus.RESERVED);
            
            if (reservations.isEmpty()) {
                log.info("No active reservations found for cancelled order: {}", orderId);
                return;
            }
            
            List<InventoryReleasedEvent.ReleasedItem> releasedItems = new ArrayList<>();
            
            for (InventoryReservation reservation : reservations) {
                try {
                    // Release the inventory
                    InventoryItem item = inventoryItemRepository.findByProductId(reservation.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + reservation.getProductId()));
                    
                    item.releaseQuantity(reservation.getQuantity());
                    inventoryItemRepository.save(item);
                    
                    // Mark reservation as released
                    reservation.markAsReleased();
                    reservationRepository.save(reservation);
                    
                    releasedItems.add(new InventoryReleasedEvent.ReleasedItem(
                        reservation.getProductId(),
                        item.getProductName(),
                        reservation.getQuantity()
                    ));
                    
                    log.info("Released {} units of product {} for cancelled order {}", 
                            reservation.getQuantity(), reservation.getProductId(), orderId);
                } catch (Exception e) {
                    log.error("Failed to release inventory for product {}: {}", 
                            reservation.getProductId(), e.getMessage());
                }
            }
            
            // Publish inventory released event
            String reservationId = UUID.randomUUID().toString();
            InventoryReleasedEvent releasedEvent = new InventoryReleasedEvent(
                orderId, 
                reservationId, 
                releasedItems, 
                "Order cancelled"
            );
            eventProducer.publishInventoryReleased(releasedEvent);
            
            log.info("Successfully released inventory for cancelled order: {}", orderId);
            
        } catch (Exception e) {
            log.error("Error processing order cancelled event for order {}: {}", 
                    event.getOrderId(), e.getMessage(), e);
        }
    }
    
    private List<ReservationRequest> createDefaultReservationRequests() {
        // For demo purposes, create some default reservation requests
        // In a real system, this would come from the order details
        return List.of(
            new ReservationRequest("PROD-001", 2),
            new ReservationRequest("PROD-002", 1),
            new ReservationRequest("PROD-003", 3)
        );
    }
    
    private static class ReservationRequest {
        private final String productId;
        private final Integer quantity;
        
        public ReservationRequest(String productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
        
        public String getProductId() { return productId; }
        public Integer getQuantity() { return quantity; }
    }
    
    /**
     * Scheduled task to clean up expired reservations
     * Runs every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    @Transactional
    public void cleanupExpiredReservations() {
        try {
            Instant now = Instant.now();
            List<InventoryReservation> expiredReservations = reservationRepository.findExpiredReservations(now);
            
            if (!expiredReservations.isEmpty()) {
                log.info("Found {} expired reservations to clean up", expiredReservations.size());
                
                for (InventoryReservation reservation : expiredReservations) {
                    try {
                        // Release the inventory
                        InventoryItem item = inventoryItemRepository.findByProductId(reservation.getProductId())
                            .orElse(null);
                        
                        if (item != null) {
                            item.releaseQuantity(reservation.getQuantity());
                            inventoryItemRepository.save(item);
                            
                            log.info("Released {} units of product {} from expired reservation {}", 
                                    reservation.getQuantity(), reservation.getProductId(), reservation.getId());
                        }
                        
                        // Mark reservation as expired
                        reservation.markAsExpired();
                        reservationRepository.save(reservation);
                        
                    } catch (Exception e) {
                        log.error("Failed to cleanup expired reservation {}: {}", 
                                reservation.getId(), e.getMessage(), e);
                    }
                }
                
                log.info("Successfully cleaned up {} expired reservations", expiredReservations.size());
            }
            
        } catch (Exception e) {
            log.error("Error during expired reservation cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Scheduled task to check for low stock items
     * Runs every hour
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour
    @Transactional
    public void checkLowStockItems() {
        try {
            List<InventoryItem> allItems = inventoryItemRepository.findAll();
            List<InventoryItem> lowStockItems = allItems.stream()
                .filter(InventoryItem::isLowStock)
                .toList();
            
            if (!lowStockItems.isEmpty()) {
                log.warn("Found {} items with low stock levels", lowStockItems.size());
                
                for (InventoryItem item : lowStockItems) {
                    log.warn("Low stock alert: Product {} ({}) has {} available, minimum is {}", 
                            item.getProductId(), item.getProductName(), 
                            item.getAvailableQuantity(), item.getMinStockLevel());
                    
                    // In a real system, you might:
                    // 1. Send notifications to warehouse staff
                    // 2. Create reorder requests
                    // 3. Update supplier systems
                    // 4. Send alerts to management
                }
            }
            
        } catch (Exception e) {
            log.error("Error checking low stock items: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get inventory health summary
     */
    public InventoryHealthSummary getInventoryHealth() {
        List<InventoryItem> allItems = inventoryItemRepository.findAll();
        
        long totalItems = allItems.size();
        long activeItems = allItems.stream().mapToLong(item -> item.getIsActive() ? 1 : 0).sum();
        long lowStockItems = allItems.stream().mapToLong(item -> item.isLowStock() ? 1 : 0).sum();
        long outOfStockItems = allItems.stream().mapToLong(item -> item.isOutOfStock() ? 1 : 0).sum();
        long discontinuedItems = allItems.stream().mapToLong(item -> item.getIsDiscontinued() ? 1 : 0).sum();
        
        return new InventoryHealthSummary(totalItems, activeItems, lowStockItems, outOfStockItems, discontinuedItems);
    }
    
    /**
     * Inventory health summary
     */
    public static class InventoryHealthSummary {
        private final long totalItems;
        private final long activeItems;
        private final long lowStockItems;
        private final long outOfStockItems;
        private final long discontinuedItems;
        
        public InventoryHealthSummary(long totalItems, long activeItems, long lowStockItems, 
                                   long outOfStockItems, long discontinuedItems) {
            this.totalItems = totalItems;
            this.activeItems = activeItems;
            this.lowStockItems = lowStockItems;
            this.outOfStockItems = outOfStockItems;
            this.discontinuedItems = discontinuedItems;
        }
        
        public long getTotalItems() { return totalItems; }
        public long getActiveItems() { return activeItems; }
        public long getLowStockItems() { return lowStockItems; }
        public long getOutOfStockItems() { return outOfStockItems; }
        public long getDiscontinuedItems() { return discontinuedItems; }
    }
}
