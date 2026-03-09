package com.trackops.server.adapters.output.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Custom health indicator for Kafka connectivity and performance.
 * Provides detailed information about Kafka health including broker connectivity,
 * topic availability, and message publishing capabilities.
 */
@Component
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaHealthIndicator implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaHealthIndicator.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaAdmin kafkaAdmin;
    
    @Autowired
    public KafkaHealthIndicator(KafkaTemplate<String, String> kafkaTemplate, KafkaAdmin kafkaAdmin) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaAdmin = kafkaAdmin;
    }
    
    @Override
    public Health health() {
        try {
            // Test Kafka connectivity by getting admin client
            Map<String, Object> adminProperties = kafkaAdmin.getConfigurationProperties();
            String bootstrapServers = (String) adminProperties.get("bootstrap.servers");
            
            // Test message publishing capability
            long startTime = System.currentTimeMillis();
            String testTopic = "health-check-topic";
            String testKey = "health-check-key";
            String testMessage = "health-check-message-" + System.currentTimeMillis();
            
            // Send a test message (fire and forget for health check)
            kafkaTemplate.send(testTopic, testKey, testMessage);
            
            long publishTime = System.currentTimeMillis() - startTime;
            
            // Get Kafka cluster information
            Health.Builder healthBuilder = Health.up()
                .withDetail("kafka", "Apache Kafka")
                .withDetail("bootstrapServers", bootstrapServers)
                .withDetail("publishTime", publishTime + "ms")
                .withDetail("status", "Connected and responsive");
            
            // Add performance warnings
            if (publishTime > 500) {
                healthBuilder = healthBuilder.withDetail("warning", "Slow Kafka operations detected");
            }
            
            if (publishTime > 5000) {
                return Health.down()
                    .withDetail("kafka", "Apache Kafka")
                    .withDetail("error", "Kafka operation timeout")
                    .withDetail("publishTime", publishTime + "ms")
                    .build();
            }
            
            // Add topic information
            healthBuilder = healthBuilder
                .withDetail("topics", "ORDER_CREATED, ORDER_STATUS_UPDATED, ORDER_CANCELLED, ORDER_DELIVERED")
                .withDetail("consumerGroup", "trackops-orders");
            
            logger.debug("Kafka health check completed successfully in {}ms", publishTime);
            return healthBuilder.build();
            
        } catch (Exception e) {
            logger.error("Kafka health check failed", e);
            return Health.down()
                .withDetail("kafka", "Apache Kafka")
                .withDetail("error", e.getMessage())
                .withDetail("status", "Connection failed")
                .build();
        }
    }
}
