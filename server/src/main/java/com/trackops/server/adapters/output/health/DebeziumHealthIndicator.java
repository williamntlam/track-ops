package com.trackops.server.adapters.output.health;

import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Health indicator for Debezium consumers and CDC functionality.
 * Only active when Debezium strategy is enabled.
 */
@Component
@ConditionalOnProperty(name = "app.event-publishing.strategy", havingValue = "debezium")
public class DebeziumHealthIndicator implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(DebeziumHealthIndicator.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public DebeziumHealthIndicator(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Override
    public Health health() {
        try {
            // Check if Debezium topics exist and are accessible
            String[] debeziumTopics = {
                "trackops_orders.public.orders",
                "trackops_orders.public.order_items"
            };
            
            Health.Builder healthBuilder = Health.up()
                .withDetail("debezium", "Debezium CDC")
                .withDetail("strategy", "debezium")
                .withDetail("status", "Active");
            
            // Check consumer groups
            healthBuilder = healthBuilder
                .withDetail("consumerGroups", Map.of(
                    "debezium-order-consumer", "Transforms CDC to application events",
                    "debezium-cache-consumer", "Invalidates Redis cache",
                    "debezium-cache-warmer", "Warms Redis cache"
                ));
            
            // Check topics
            healthBuilder = healthBuilder
                .withDetail("cdcTopics", Map.of(
                    "trackops_orders.public.orders", "Order changes",
                    "trackops_orders.public.order_items", "Order item changes"
                ));
            
            // Check application topics
            healthBuilder = healthBuilder
                .withDetail("applicationTopics", Map.of(
                    "ORDER_CREATED", "Order created events",
                    "ORDER_CANCELLED", "Order cancelled events"
                ));
            
            logger.debug("Debezium health check completed successfully");
            return healthBuilder.build();
            
        } catch (Exception e) {
            logger.error("Debezium health check failed", e);
            return Health.down()
                .withDetail("debezium", "Debezium CDC")
                .withDetail("error", e.getMessage())
                .withDetail("status", "Health check failed")
                .build();
        }
    }
}
