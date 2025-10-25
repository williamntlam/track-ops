package com.trackops.server.adapters.output.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Service for structured logging with business context and monitoring integration.
 * Provides methods for logging business events, performance metrics, and system events.
 */
@Service
public class StructuredLoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(StructuredLoggingService.class);
    private static final Logger businessLogger = LoggerFactory.getLogger("com.trackops.server.application.services");
    private static final Logger accessLogger = LoggerFactory.getLogger("com.trackops.server.adapters.input.web");
    private static final Logger cacheLogger = LoggerFactory.getLogger("com.trackops.server.adapters.output.cache");
    private static final Logger healthLogger = LoggerFactory.getLogger("com.trackops.server.adapters.output.health");
    private static final Logger monitoringLogger = LoggerFactory.getLogger("com.trackops.server.adapters.output.monitoring");
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    
    // ==================== BUSINESS LOGGING ====================
    
    /**
     * Log business event with structured data.
     */
    public void logBusinessEvent(String event, String entityType, String entityId, Map<String, Object> context) {
        try {
            MDC.put("event", event);
            MDC.put("entityType", entityType);
            MDC.put("entityId", entityId);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            
            if (context != null) {
                context.forEach((key, contextValue) -> MDC.put(key, String.valueOf(contextValue)));
            }
            
            businessLogger.info("Business event: {} for {} {}", event, entityType, entityId);
            
        } finally {
            // Clean up context-specific MDC entries
            MDC.remove("event");
            MDC.remove("entityType");
            MDC.remove("entityId");
            MDC.remove("timestamp");
            if (context != null) {
                context.keySet().forEach(MDC::remove);
            }
        }
    }
    
    /**
     * Log order-related business events.
     */
    public void logOrderEvent(String event, String orderId, String customerId, Map<String, Object> context) {
        try {
            MDC.put("event", event);
            MDC.put("entityType", "order");
            MDC.put("entityId", orderId);
            MDC.put("customerId", customerId);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            
            if (context != null) {
                context.forEach((key, contextValue) -> MDC.put(key, String.valueOf(contextValue)));
            }
            
            businessLogger.info("Order event: {} for order {} (customer: {})", event, orderId, customerId);
            
        } finally {
            // Clean up context-specific MDC entries
            MDC.remove("event");
            MDC.remove("entityType");
            MDC.remove("entityId");
            MDC.remove("customerId");
            MDC.remove("timestamp");
            if (context != null) {
                context.keySet().forEach(MDC::remove);
            }
        }
    }
    
    /**
     * Log SAGA-related business events.
     */
    public void logSagaEvent(String event, String sagaId, String sagaType, Map<String, Object> context) {
        try {
            MDC.put("event", event);
            MDC.put("entityType", "saga");
            MDC.put("entityId", sagaId);
            MDC.put("sagaType", sagaType);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            
            if (context != null) {
                context.forEach((key, contextValue) -> MDC.put(key, String.valueOf(contextValue)));
            }
            
            businessLogger.info("SAGA event: {} for {} {}", event, sagaType, sagaId);
            
        } finally {
            // Clean up context-specific MDC entries
            MDC.remove("event");
            MDC.remove("entityType");
            MDC.remove("entityId");
            MDC.remove("sagaType");
            MDC.remove("timestamp");
            if (context != null) {
                context.keySet().forEach(MDC::remove);
            }
        }
    }
    
    // ==================== ACCESS LOGGING ====================
    
    /**
     * Log API access with structured data.
     */
    public void logApiAccess(String method, String endpoint, int statusCode, long durationMs, 
                           String userAgent, String clientIp) {
        try {
            MDC.put("httpMethod", method);
            MDC.put("endpoint", endpoint);
            MDC.put("statusCode", String.valueOf(statusCode));
            MDC.put("durationMs", String.valueOf(durationMs));
            MDC.put("userAgent", userAgent);
            MDC.put("clientIp", clientIp);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            
            accessLogger.info("API access: {} {} -> {} ({}ms)", method, endpoint, statusCode, durationMs);
            
        } finally {
            // Clean up context-specific MDC entries
            MDC.remove("httpMethod");
            MDC.remove("endpoint");
            MDC.remove("statusCode");
            MDC.remove("durationMs");
            MDC.remove("userAgent");
            MDC.remove("clientIp");
            MDC.remove("timestamp");
        }
    }
    
    /**
     * Log API error with structured data.
     */
    public void logApiError(String method, String endpoint, int statusCode, String errorMessage, 
                          String userAgent, String clientIp) {
        try {
            MDC.put("httpMethod", method);
            MDC.put("endpoint", endpoint);
            MDC.put("statusCode", String.valueOf(statusCode));
            MDC.put("errorMessage", errorMessage);
            MDC.put("userAgent", userAgent);
            MDC.put("clientIp", clientIp);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            
            accessLogger.error("API error: {} {} -> {} - {}", method, endpoint, statusCode, errorMessage);
            
        } finally {
            // Clean up context-specific MDC entries
            MDC.remove("httpMethod");
            MDC.remove("endpoint");
            MDC.remove("statusCode");
            MDC.remove("errorMessage");
            MDC.remove("userAgent");
            MDC.remove("clientIp");
            MDC.remove("timestamp");
        }
    }
    
    // ==================== CACHE LOGGING ====================
    
    /**
     * Log cache operations with structured data.
     */
    public void logCacheOperation(String operation, String cacheName, String key, boolean success, 
                                long durationMs, String errorMessage) {
        try {
            MDC.put("operation", operation);
            MDC.put("cacheName", cacheName);
            MDC.put("cacheKey", key);
            MDC.put("success", String.valueOf(success));
            MDC.put("durationMs", String.valueOf(durationMs));
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            
            if (errorMessage != null) {
                MDC.put("errorMessage", errorMessage);
            }
            
            if (success) {
                cacheLogger.debug("Cache {}: {} {} ({}ms)", operation, cacheName, key, durationMs);
            } else {
                cacheLogger.warn("Cache {} failed: {} {} ({}ms) - {}", operation, cacheName, key, durationMs, errorMessage);
            }
            
        } finally {
            // Clean up context-specific MDC entries
            MDC.remove("operation");
            MDC.remove("cacheName");
            MDC.remove("cacheKey");
            MDC.remove("success");
            MDC.remove("durationMs");
            MDC.remove("errorMessage");
            MDC.remove("timestamp");
        }
    }
    
    // ==================== HEALTH LOGGING ====================
    
    /**
     * Log health check events with structured data.
     */
    public void logHealthCheck(String component, String status, long durationMs, String details) {
        try {
            MDC.put("component", component);
            MDC.put("status", status);
            MDC.put("durationMs", String.valueOf(durationMs));
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            
            if (details != null) {
                MDC.put("details", details);
            }
            
            if ("UP".equals(status)) {
                healthLogger.debug("Health check: {} is {} ({}ms)", component, status, durationMs);
            } else {
                healthLogger.warn("Health check: {} is {} ({}ms) - {}", component, status, durationMs, details);
            }
            
        } finally {
            // Clean up context-specific MDC entries
            MDC.remove("component");
            MDC.remove("status");
            MDC.remove("durationMs");
            MDC.remove("details");
            MDC.remove("timestamp");
        }
    }
    
    // ==================== MONITORING LOGGING ====================
    
    /**
     * Log monitoring events with structured data.
     */
    public void logMonitoringEvent(String event, String metric, Object value, Map<String, Object> context) {
        try {
            MDC.put("event", event);
            MDC.put("metric", metric);
            MDC.put("value", String.valueOf(value));
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            
            if (context != null) {
                context.forEach((key, contextValue) -> MDC.put(key, String.valueOf(contextValue)));
            }
            
            monitoringLogger.info("Monitoring event: {} - {} = {}", event, metric, value);
            
        } finally {
            // Clean up context-specific MDC entries
            MDC.remove("event");
            MDC.remove("metric");
            MDC.remove("value");
            MDC.remove("timestamp");
            if (context != null) {
                context.keySet().forEach(MDC::remove);
            }
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Log performance metrics with structured data.
     */
    public void logPerformanceMetric(String operation, long durationMs, String status, Map<String, Object> context) {
        try {
            MDC.put("operation", operation);
            MDC.put("durationMs", String.valueOf(durationMs));
            MDC.put("status", status);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            
            if (context != null) {
                context.forEach((key, contextValue) -> MDC.put(key, String.valueOf(contextValue)));
            }
            
            logger.info("Performance: {} completed in {}ms with status {}", operation, durationMs, status);
            
        } finally {
            // Clean up context-specific MDC entries
            MDC.remove("operation");
            MDC.remove("durationMs");
            MDC.remove("status");
            MDC.remove("timestamp");
            if (context != null) {
                context.keySet().forEach(MDC::remove);
            }
        }
    }
    
    /**
     * Log system events with structured data.
     */
    public void logSystemEvent(String event, String level, String message, Map<String, Object> context) {
        try {
            MDC.put("event", event);
            MDC.put("level", level);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            
            if (context != null) {
                context.forEach((key, contextValue) -> MDC.put(key, String.valueOf(contextValue)));
            }
            
            switch (level.toUpperCase()) {
                case "ERROR":
                    logger.error("System event: {} - {}", event, message);
                    break;
                case "WARN":
                    logger.warn("System event: {} - {}", event, message);
                    break;
                case "INFO":
                default:
                    logger.info("System event: {} - {}", event, message);
                    break;
            }
            
        } finally {
            // Clean up context-specific MDC entries
            MDC.remove("event");
            MDC.remove("level");
            MDC.remove("timestamp");
            if (context != null) {
                context.keySet().forEach(MDC::remove);
            }
        }
    }
}
