package com.trackops.inventory.adapters.input.web.controllers;

import com.trackops.inventory.adapters.input.web.dto.*;
import com.trackops.inventory.adapters.input.web.exception.ErrorResponse;
import com.trackops.inventory.application.services.InventoryService;
import com.trackops.inventory.domain.model.InventoryItem;
import com.trackops.inventory.ports.output.persistence.InventoryItemRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/inventory/items")
@RequiredArgsConstructor
public class InventoryController {
    
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryService inventoryService;
    
    /**
     * Get all inventory items with pagination
     */
    @GetMapping
    public ResponseEntity<?> getAllItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "productName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String category) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<InventoryItem> items;
            if (category != null && !category.trim().isEmpty()) {
                items = inventoryItemRepository.findByCategory(category, pageable);
            } else {
                items = inventoryItemRepository.findAll(pageable);
            }
            
            Page<InventoryItemResponse> response = items.map(InventoryItemResponse::from);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving inventory items: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to retrieve inventory items: " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get inventory item by product ID
     */
    @GetMapping("/{productId}")
    public ResponseEntity<?> getItemByProductId(@PathVariable String productId) {
        try {
            return inventoryItemRepository.findByProductId(productId)
                .map(InventoryItemResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            log.error("Error retrieving inventory item for product ID {}: {}", productId, e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to retrieve inventory item for product ID " + productId + ": " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get available inventory items
     */
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableItems(
            @RequestParam(required = false) Integer minQuantity) {
        
        try {
            List<InventoryItem> items;
            if (minQuantity != null) {
                items = inventoryItemRepository.findAvailableItemsWithQuantity(minQuantity);
            } else {
                items = inventoryItemRepository.findAvailableItems();
            }
            
            List<InventoryItemResponse> response = items.stream()
                .map(InventoryItemResponse::from)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving available inventory items: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to retrieve available inventory items: " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Create new inventory item
     */
    @PostMapping
    public ResponseEntity<?> createItem(
            @Valid @RequestBody CreateInventoryItemRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("Creating inventory item for product ID: {}", request.getProductId());
            
            // Check if product ID already exists
            if (inventoryItemRepository.existsByProductId(request.getProductId())) {
                log.warn("Product ID already exists: {}", request.getProductId());
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(Instant.now())
                    .status(HttpStatus.CONFLICT.value())
                    .error("Conflict")
                    .message("Product ID already exists: " + request.getProductId())
                    .path(httpRequest.getRequestURI())
                    .build();
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }
            
            InventoryItem item = InventoryItem.builder()
                .productId(request.getProductId())
                .productName(request.getProductName())
                .description(request.getDescription())
                .availableQuantity(request.getAvailableQuantity())
                .reservedQuantity(0)
                .unitPrice(request.getUnitPrice())
                .category(request.getCategory())
                .minStockLevel(request.getMinStockLevel())
                .maxStockLevel(request.getMaxStockLevel())
                .reorderQuantity(request.getReorderQuantity())
                .isActive(true)
                .isDiscontinued(false)
                .build();
            
            InventoryItem savedItem = inventoryItemRepository.save(item);
            log.info("Successfully created inventory item: {} for product: {}", savedItem.getId(), request.getProductId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(InventoryItemResponse.from(savedItem));
                
        } catch (Exception e) {
            log.error("Error creating inventory item for product ID {}: {}", request.getProductId(), e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to create inventory item: " + e.getMessage())
                .path(httpRequest.getRequestURI())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Update inventory item
     */
    @PutMapping("/{productId}")
    public ResponseEntity<?> updateItem(
            @PathVariable String productId,
            @Valid @RequestBody UpdateInventoryItemRequest request) {
        
        try {
            return inventoryItemRepository.findByProductId(productId)
                .map(item -> {
                    if (request.getProductName() != null) {
                        item.setProductName(request.getProductName());
                    }
                    if (request.getDescription() != null) {
                        item.setDescription(request.getDescription());
                    }
                    if (request.getAvailableQuantity() != null) {
                        item.setAvailableQuantity(request.getAvailableQuantity());
                    }
                    if (request.getUnitPrice() != null) {
                        item.setUnitPrice(request.getUnitPrice());
                    }
                    if (request.getCategory() != null) {
                        item.setCategory(request.getCategory());
                    }
                    if (request.getMinStockLevel() != null) {
                        item.setMinStockLevel(request.getMinStockLevel());
                    }
                    if (request.getMaxStockLevel() != null) {
                        item.setMaxStockLevel(request.getMaxStockLevel());
                    }
                    if (request.getReorderQuantity() != null) {
                        item.setReorderQuantity(request.getReorderQuantity());
                    }
                    if (request.getIsActive() != null) {
                        item.setIsActive(request.getIsActive());
                    }
                    if (request.getIsDiscontinued() != null) {
                        if (request.getIsDiscontinued()) {
                            item.discontinue();
                        } else {
                            item.reactivate();
                        }
                    }
                    
                    InventoryItem updatedItem = inventoryItemRepository.save(item);
                    log.info("Updated inventory item: {} for product: {}", updatedItem.getId(), productId);
                    return ResponseEntity.ok(InventoryItemResponse.from(updatedItem));
                })
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            log.error("Error updating inventory item for product ID {}: {}", productId, e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to update inventory item for product ID " + productId + ": " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Delete inventory item
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<?> deleteItem(@PathVariable String productId) {
        try {
            return inventoryItemRepository.findByProductId(productId)
                .map(item -> {
                    inventoryItemRepository.delete(item);
                    log.info("Deleted inventory item for product: {}", productId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            log.error("Error deleting inventory item for product ID {}: {}", productId, e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to delete inventory item for product ID " + productId + ": " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get inventory statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getInventoryStats() {
        try {
            List<InventoryItem> allItems = inventoryItemRepository.findAll();
            
            long totalItems = allItems.size();
            long availableItems = allItems.stream()
                .mapToLong(item -> item.getAvailableQuantity())
                .sum();
            long reservedItems = allItems.stream()
                .mapToLong(item -> item.getReservedQuantity())
                .sum();
            long outOfStockItems = allItems.stream()
                .mapToLong(item -> item.getAvailableQuantity() == 0 ? 1 : 0)
                .sum();
            
            InventoryStatsResponse stats = InventoryStatsResponse.builder()
                .totalItems(totalItems)
                .totalAvailableQuantity(availableItems)
                .totalReservedQuantity(reservedItems)
                .outOfStockItems(outOfStockItems)
                .build();
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error retrieving inventory statistics: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to retrieve inventory statistics: " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get inventory health summary
     */
    @GetMapping("/health")
    public ResponseEntity<?> getInventoryHealth() {
        try {
            var healthSummary = inventoryService.getInventoryHealth();
            
            Map<String, Object> healthStatus = new HashMap<>();
            healthStatus.put("totalItems", healthSummary.getTotalItems());
            healthStatus.put("activeItems", healthSummary.getActiveItems());
            healthStatus.put("lowStockItems", healthSummary.getLowStockItems());
            healthStatus.put("outOfStockItems", healthSummary.getOutOfStockItems());
            healthStatus.put("discontinuedItems", healthSummary.getDiscontinuedItems());
            healthStatus.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(healthStatus);
            
        } catch (Exception e) {
            log.error("Error retrieving inventory health: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to retrieve inventory health: " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
