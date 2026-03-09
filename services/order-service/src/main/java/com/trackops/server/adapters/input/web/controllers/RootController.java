package com.trackops.server.adapters.input.web.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Root controller to handle requests to the base path "/".
 * Provides API information and available endpoints.
 */
@RestController
public class RootController {
    
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "TrackOps Order Service");
        response.put("version", "1.0.0");
        response.put("status", "UP");
        response.put("message", "Welcome to TrackOps Order Service API");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("health", "/actuator/health");
        endpoints.put("orders", "/api/orders");
        endpoints.put("health_check", "/api/health/simple");
        endpoints.put("actuator", "/actuator");
        
        response.put("endpoints", endpoints);
        
        return ResponseEntity.ok(response);
    }
}
