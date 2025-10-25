package com.trackops.inventory.adapters.input.web.controllers;

import com.trackops.inventory.adapters.input.web.dto.*;
import com.trackops.inventory.application.services.InventoryService;
import com.trackops.inventory.domain.model.InventoryItem;
import com.trackops.inventory.ports.output.persistence.InventoryItemRepository;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/inventory/items")
@RequiredArgsConstructor
public class InventoryController {
    
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryService inventoryService;
    
    /**
     * Get all inventory items with pagination
     */
    @GetMapping
    public ResponseEntity<Page<InventoryItemResponse>> getAllItems(
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get inventory item by product ID
     */
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryItemResponse> getItemByProductId(@PathVariable String productId) {
        try {
            return inventoryItemRepository.findByProductId(productId)
                .map(InventoryItemResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            log.error("Error retrieving inventory item for product ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get available inventory items
     */
    @GetMapping("/available")
    public ResponseEntity<List<InventoryItemResponse>> getAvailableItems(
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Create new inventory item
     */
    @PostMapping
    public ResponseEntity<InventoryItemResponse> createItem(@Valid @RequestBody CreateInventoryItemRequest request) {
        try {
            // Check if product ID already exists
            if (inventoryItemRepository.existsByProductId(request.getProductId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            
            InventoryItem item = InventoryItem.builder()
                .productId(request.getProductId())
                .productName(request.getProductName())
                .description(request.getDescription())
                .availableQuantity(request.getAvailableQuantity())
                .reservedQuantity(0)
                .unitPrice(request.getUnitPrice())
                .category(request.getCategory())
                .build();
            
            InventoryItem savedItem = inventoryItemRepository.save(item);
            log.info("Created inventory item: {} for product: {}", savedItem.getId(), request.getProductId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(InventoryItemResponse.from(savedItem));
                
        } catch (Exception e) {
            log.error("Error creating inventory item: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update inventory item
     */
    @PutMapping("/{productId}")
    public ResponseEntity<InventoryItemResponse> updateItem(
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
                    
                    InventoryItem updatedItem = inventoryItemRepository.save(item);
                    log.info("Updated inventory item: {} for product: {}", updatedItem.getId(), productId);
                    return ResponseEntity.ok(InventoryItemResponse.from(updatedItem));
                })
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            log.error("Error updating inventory item for product ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete inventory item
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteItem(@PathVariable String productId) {
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get inventory statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<InventoryStatsResponse> getInventoryStats() {
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
