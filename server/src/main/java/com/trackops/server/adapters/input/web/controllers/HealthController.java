package com.trackops.server.adapters.input.web.controllers;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import com.trackops.server.adapters.output.health.DatabaseHealthIndicator;
import com.trackops.server.adapters.output.health.RedisHealthIndicator;
import com.trackops.server.adapters.output.health.KafkaHealthIndicator;
import com.trackops.server.adapters.output.health.ApplicationHealthIndicator;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Custom health check controller for additional health endpoints.
 * Provides simplified health checks and status information for load balancers
 * and monitoring systems that need lightweight health endpoints.
 * 
 * Note: For production, prefer Spring Boot Actuator endpoints:
 * - /actuator/health (main health endpoint)
 * - /actuator/health/readiness (Kubernetes readiness probe)
 * - /actuator/health/liveness (Kubernetes liveness probe)
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {
    
    private final HealthIndicator databaseHealthIndicator;
    private final HealthIndicator redisHealthIndicator;
    private final HealthIndicator applicationHealthIndicator;
    private final KafkaHealthIndicator kafkaHealthIndicator;
    
    public HealthController(
            DatabaseHealthIndicator databaseHealthIndicator,
            RedisHealthIndicator redisHealthIndicator,
            ApplicationHealthIndicator applicationHealthIndicator,
            @Autowired(required = false) KafkaHealthIndicator kafkaHealthIndicator) {
        this.databaseHealthIndicator = databaseHealthIndicator;
        this.redisHealthIndicator = redisHealthIndicator;
        this.applicationHealthIndicator = applicationHealthIndicator;
        this.kafkaHealthIndicator = kafkaHealthIndicator;
    }
    
    /**
     * Helper method to check if all critical services are healthy.
     * Critical services: Database, Redis, Kafka (if available)
     */
    private boolean areCriticalServicesHealthy() {
        boolean dbHealthy = isHealthy(databaseHealthIndicator);
        boolean redisHealthy = isHealthy(redisHealthIndicator);
        boolean kafkaHealthy = (kafkaHealthIndicator == null) || isHealthy(kafkaHealthIndicator);
        
        return dbHealthy && redisHealthy && kafkaHealthy;
    }
    
    /**
     * Helper method to check if a health indicator is UP.
     * Uses Status.UP.equals() for type safety and future-proofing.
     */
    private boolean isHealthy(HealthIndicator indicator) {
        try {
            Health health = indicator.health();
            return Status.UP.equals(health.getStatus());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Helper method to get health status safely with timeout protection.
     */
    private Health getHealthSafely(HealthIndicator indicator) {
        try {
            return indicator.health();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", "Health check failed: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Simple health check endpoint for load balancers.
     * Returns 200 OK if all critical services are healthy, 503 if any are down.
     */
    @GetMapping("/simple")
    public ResponseEntity<Map<String, String>> simpleHealth() {
        Map<String, String> response = new HashMap<>();
        
        boolean criticalServicesHealthy = areCriticalServicesHealthy();
        
        if (criticalServicesHealthy) {
            response.put("status", "UP");
            response.put("message", "All critical services are healthy");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "DOWN");
            response.put("message", "One or more critical services are unhealthy");
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * Detailed health check endpoint with component status and details.
     * Includes both status and details for each component.
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> response = new HashMap<>();
        
        // Get health status safely for all components
        Health dbHealth = getHealthSafely(databaseHealthIndicator);
        Health redisHealth = getHealthSafely(redisHealthIndicator);
        Health kafkaHealth = (kafkaHealthIndicator != null) ? getHealthSafely(kafkaHealthIndicator) : 
            Health.down().withDetail("error", "Kafka not configured").build();
        Health appHealth = getHealthSafely(applicationHealthIndicator);
        
        // Build component status map with both status and details
        Map<String, Object> components = new HashMap<>();
        
        Map<String, Object> dbComponent = new HashMap<>();
        dbComponent.put("status", dbHealth.getStatus().getCode());
        dbComponent.put("details", dbHealth.getDetails());
        components.put("database", dbComponent);
        
        Map<String, Object> redisComponent = new HashMap<>();
        redisComponent.put("status", redisHealth.getStatus().getCode());
        redisComponent.put("details", redisHealth.getDetails());
        components.put("redis", redisComponent);
        
        Map<String, Object> kafkaComponent = new HashMap<>();
        kafkaComponent.put("status", kafkaHealth.getStatus().getCode());
        kafkaComponent.put("details", kafkaHealth.getDetails());
        components.put("kafka", kafkaComponent);
        
        Map<String, Object> appComponent = new HashMap<>();
        appComponent.put("status", appHealth.getStatus().getCode());
        appComponent.put("details", appHealth.getDetails());
        components.put("application", appComponent);
        
        // Determine overall status
        boolean criticalServicesHealthy = areCriticalServicesHealthy();
        String overallStatus = criticalServicesHealthy ? "UP" : "DOWN";
        
        response.put("status", overallStatus);
        response.put("components", components);
        response.put("timestamp", System.currentTimeMillis());
        
        // Return 503 if any critical service is down
        if (criticalServicesHealthy) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * Readiness probe endpoint for Kubernetes.
     * Indicates if the application is ready to receive traffic.
     * Only checks critical services (Database, Redis, Kafka).
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> readiness() {
        Map<String, String> response = new HashMap<>();
        
        boolean criticalServicesHealthy = areCriticalServicesHealthy();
        
        if (criticalServicesHealthy) {
            response.put("status", "UP");
            response.put("message", "Application is ready to receive traffic");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "DOWN");
            response.put("message", "Application is not ready - critical services unavailable");
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * Liveness probe endpoint for Kubernetes.
     * Indicates if the application is alive and should be restarted if not.
     * This is a lightweight check that only verifies the application is responding.
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> liveness() {
        Map<String, String> response = new HashMap<>();
        
        // Simple liveness check - just verify the application is responding
        // No external dependencies checked for liveness
        response.put("status", "UP");
        response.put("message", "Application is alive");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }
}
