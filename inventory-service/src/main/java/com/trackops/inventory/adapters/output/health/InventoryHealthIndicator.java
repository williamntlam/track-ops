package com.trackops.inventory.adapters.output.health;

import com.trackops.inventory.ports.output.persistence.InventoryItemRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Custom health indicator for Inventory Service.
 * Provides detailed information about inventory health including database connectivity,
 * inventory levels, and service performance.
 */
@Component
@ConditionalOnBean(InventoryItemRepository.class)
public class InventoryHealthIndicator implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryHealthIndicator.class);
    
    private final InventoryItemRepository inventoryItemRepository;
    
    @Autowired
    public InventoryHealthIndicator(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }
    
    @Override
    public Health health() {
        try {
            // Test database connectivity
            long totalItems = inventoryItemRepository.count();
            long availableItems = inventoryItemRepository.findAvailableItems().size();
            long lowStockItems = inventoryItemRepository.findAvailableItemsWithQuantity(5).size();
            
            // Get inventory statistics
            Health.Builder healthBuilder = Health.up()
                .withDetail("service", "Inventory Service")
                .withDetail("totalItems", totalItems)
                .withDetail("availableItems", availableItems)
                .withDetail("lowStockItems", lowStockItems)
                .withDetail("status", "Healthy");
            
            // Add warnings for low stock
            if (lowStockItems > 0) {
                healthBuilder = healthBuilder.withDetail("warning", 
                    lowStockItems + " items have low stock (≤5 units)");
            }
            
            // Add critical warning if no items available
            if (availableItems == 0) {
                return Health.down()
                    .withDetail("service", "Inventory Service")
                    .withDetail("error", "No inventory items available")
                    .withDetail("totalItems", totalItems)
                    .withDetail("availableItems", availableItems)
                    .build();
            }
            
            logger.debug("Inventory health check completed successfully. Total items: {}, Available: {}", 
                        totalItems, availableItems);
            return healthBuilder.build();
            
        } catch (Exception e) {
            logger.error("Inventory health check failed", e);
            return Health.down()
                .withDetail("service", "Inventory Service")
                .withDetail("error", e.getMessage())
                .withDetail("status", "Database connection failed")
                .build();
        }
    }
}
