package com.trackops.server.adapters.input.web.controllers;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.trackops.server.adapters.output.health.DatabaseHealthIndicator;
import com.trackops.server.adapters.output.health.RedisHealthIndicator;
import com.trackops.server.adapters.output.health.KafkaHealthIndicator;
import com.trackops.server.adapters.output.health.ApplicationHealthIndicator;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom health check controller for additional health endpoints.
 * Provides simplified health checks and status information for load balancers
 * and monitoring systems that need lightweight health endpoints.
 */
@RestController
@RequestMapping("/health")
public class HealthController {
    
    private final HealthIndicator databaseHealthIndicator;
    private final HealthIndicator redisHealthIndicator;
    private final HealthIndicator kafkaHealthIndicator;
    private final HealthIndicator applicationHealthIndicator;
    
    public HealthController(
            DatabaseHealthIndicator databaseHealthIndicator,
            RedisHealthIndicator redisHealthIndicator,
            KafkaHealthIndicator kafkaHealthIndicator,
            ApplicationHealthIndicator applicationHealthIndicator) {
        this.databaseHealthIndicator = databaseHealthIndicator;
        this.redisHealthIndicator = redisHealthIndicator;
        this.kafkaHealthIndicator = kafkaHealthIndicator;
        this.applicationHealthIndicator = applicationHealthIndicator;
    }
    
    /**
     * Simple health check endpoint for load balancers.
     * Returns 200 OK if all critical services are healthy.
     */
    @GetMapping("/simple")
    public ResponseEntity<Map<String, String>> simpleHealth() {
        Map<String, String> response = new HashMap<>();
        
        try {
            Health dbHealth = databaseHealthIndicator.health();
            Health redisHealth = redisHealthIndicator.health();
            Health kafkaHealth = kafkaHealthIndicator.health();
            Health appHealth = applicationHealthIndicator.health();
            
            boolean allHealthy = dbHealth.getStatus().getCode().equals("UP") &&
                               redisHealth.getStatus().getCode().equals("UP") &&
                               kafkaHealth.getStatus().getCode().equals("UP") &&
                               appHealth.getStatus().getCode().equals("UP");
            
            if (allHealthy) {
                response.put("status", "UP");
                response.put("message", "All services are healthy");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "DOWN");
                response.put("message", "One or more services are unhealthy");
                return ResponseEntity.status(503).body(response);
            }
            
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("message", "Health check failed: " + e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * Detailed health check endpoint with component status.
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Health dbHealth = databaseHealthIndicator.health();
            Health redisHealth = redisHealthIndicator.health();
            Health kafkaHealth = kafkaHealthIndicator.health();
            Health appHealth = applicationHealthIndicator.health();
            
            Map<String, Object> components = new HashMap<>();
            components.put("database", dbHealth.getDetails());
            components.put("redis", redisHealth.getDetails());
            components.put("kafka", kafkaHealth.getDetails());
            components.put("application", appHealth.getDetails());
            
            boolean allHealthy = dbHealth.getStatus().getCode().equals("UP") &&
                               redisHealth.getStatus().getCode().equals("UP") &&
                               kafkaHealth.getStatus().getCode().equals("UP") &&
                               appHealth.getStatus().getCode().equals("UP");
            
            response.put("status", allHealthy ? "UP" : "DOWN");
            response.put("components", components);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * Readiness probe endpoint for Kubernetes.
     * Indicates if the application is ready to receive traffic.
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> readiness() {
        Map<String, String> response = new HashMap<>();
        
        try {
            // Check if critical services are available
            Health dbHealth = databaseHealthIndicator.health();
            Health redisHealth = redisHealthIndicator.health();
            Health kafkaHealth = kafkaHealthIndicator.health();
            
            boolean ready = dbHealth.getStatus().getCode().equals("UP") &&
                          redisHealth.getStatus().getCode().equals("UP") &&
                          kafkaHealth.getStatus().getCode().equals("UP");
            
            if (ready) {
                response.put("status", "READY");
                response.put("message", "Application is ready to receive traffic");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "NOT_READY");
                response.put("message", "Application is not ready");
                return ResponseEntity.status(503).body(response);
            }
            
        } catch (Exception e) {
            response.put("status", "NOT_READY");
            response.put("message", "Readiness check failed: " + e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * Liveness probe endpoint for Kubernetes.
     * Indicates if the application is alive and should be restarted if not.
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> liveness() {
        Map<String, String> response = new HashMap<>();
        
        try {
            // Simple liveness check - just verify the application is responding
            response.put("status", "ALIVE");
            response.put("message", "Application is alive");
            response.put("timestamp", String.valueOf(System.currentTimeMillis()));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "DEAD");
            response.put("message", "Application is not responding");
            return ResponseEntity.status(503).body(response);
        }
    }
}
