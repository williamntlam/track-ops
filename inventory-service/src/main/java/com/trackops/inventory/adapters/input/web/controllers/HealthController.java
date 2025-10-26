package com.trackops.inventory.adapters.input.web.controllers;

import com.trackops.inventory.adapters.output.health.InventoryHealthIndicator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/inventory/health")
@RequiredArgsConstructor
public class HealthController {
    
    private final InventoryHealthIndicator inventoryHealthIndicator;
    
    /**
     * Get detailed health status
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        try {
            Map<String, Object> healthStatus = new HashMap<>();
            
            // Get inventory-specific health
            var health = inventoryHealthIndicator.health();
            healthStatus.put("inventory", health.getStatus().getCode());
            
            // Add timestamp
            healthStatus.put("timestamp", System.currentTimeMillis());
            
            // Add service info
            healthStatus.put("service", "inventory-service");
            healthStatus.put("version", "1.0.0");
            
            return ResponseEntity.ok(healthStatus);
            
        } catch (Exception e) {
            log.error("Error retrieving health status: {}", e.getMessage(), e);
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("status", "DOWN");
            errorStatus.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorStatus);
        }
    }
    
    /**
     * Simple health check endpoint
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Inventory service is running");
        return ResponseEntity.ok(response);
    }
}
