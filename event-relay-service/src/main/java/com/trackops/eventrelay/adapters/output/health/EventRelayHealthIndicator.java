package com.trackops.eventrelay.adapters.output.health;

import com.trackops.eventrelay.application.services.EventRelayService;
import com.trackops.eventrelay.ports.output.persistence.OutboxEventRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Custom health indicator for Event Relay Service.
 * Provides detailed information about event relay health including database connectivity,
 * event processing status, and service performance.
 */
@Component
@ConditionalOnBean(OutboxEventRepository.class)
public class EventRelayHealthIndicator implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(EventRelayHealthIndicator.class);
    
    private final OutboxEventRepository outboxEventRepository;
    private final EventRelayService eventRelayService;
    
    @Autowired
    public EventRelayHealthIndicator(OutboxEventRepository outboxEventRepository,
                                   EventRelayService eventRelayService) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventRelayService = eventRelayService;
    }
    
    @Override
    public Health health() {
        try {
            // Get event relay statistics
            EventRelayService.EventRelayStats stats = eventRelayService.getStats();
            
            // Test database connectivity
            long totalEvents = stats.getTotalCount();
            long unprocessedEvents = stats.getUnprocessedCount();
            long processedEvents = stats.getProcessedCount();
            long failedEvents = stats.getFailedCount();
            
            // Calculate processing rate
            double processingRate = totalEvents > 0 ? (double) processedEvents / totalEvents * 100 : 0;
            double failureRate = totalEvents > 0 ? (double) failedEvents / totalEvents * 100 : 0;
            
            // Build health response
            Health.Builder healthBuilder = Health.up()
                .withDetail("service", "Event Relay Service")
                .withDetail("totalEvents", totalEvents)
                .withDetail("unprocessedEvents", unprocessedEvents)
                .withDetail("processedEvents", processedEvents)
                .withDetail("failedEvents", failedEvents)
                .withDetail("processingRate", String.format("%.2f%%", processingRate))
                .withDetail("failureRate", String.format("%.2f%%", failureRate))
                .withDetail("status", "Healthy");
            
            // Add warnings for high failure rate
            if (failureRate > 10) {
                healthBuilder = healthBuilder.withDetail("warning", 
                    "High failure rate detected: " + String.format("%.2f%%", failureRate));
            }
            
            // Add warnings for backlog
            if (unprocessedEvents > 100) {
                healthBuilder = healthBuilder.withDetail("warning", 
                    "Large backlog detected: " + unprocessedEvents + " unprocessed events");
            }
            
            // Mark as down if failure rate is too high
            if (failureRate > 50) {
                return Health.down()
                    .withDetail("service", "Event Relay Service")
                    .withDetail("error", "High failure rate: " + String.format("%.2f%%", failureRate))
                    .withDetail("totalEvents", totalEvents)
                    .withDetail("failedEvents", failedEvents)
                    .build();
            }
            
            logger.debug("Event relay health check completed successfully. Total: {}, Processed: {}, Failed: {}", 
                        totalEvents, processedEvents, failedEvents);
            return healthBuilder.build();
            
        } catch (Exception e) {
            logger.error("Event relay health check failed", e);
            return Health.down()
                .withDetail("service", "Event Relay Service")
                .withDetail("error", e.getMessage())
                .withDetail("status", "Database connection failed")
                .build();
        }
    }
}
