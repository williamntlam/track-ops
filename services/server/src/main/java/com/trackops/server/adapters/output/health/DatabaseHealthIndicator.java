package com.trackops.server.adapters.output.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Custom health indicator for database connectivity and performance.
 * Provides detailed information about database health including connection status,
 * query performance, and basic database statistics.
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthIndicator.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public Health health() {
        try {
            // Test basic connectivity
            String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
            
            // Test query performance
            long startTime = System.currentTimeMillis();
            Integer orderCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
            long queryTime = System.currentTimeMillis() - startTime;
            
            // Get database statistics
            Map<String, Object> dbStats = jdbcTemplate.queryForMap(
                "SELECT " +
                "  (SELECT COUNT(*) FROM orders) as total_orders, " +
                "  (SELECT COUNT(*) FROM outbox_events) as total_events, " +
                "  (SELECT COUNT(*) FROM saga_instances) as total_sagas"
            );
            
            // Determine health status based on query performance
            Health.Builder healthBuilder = Health.up()
                .withDetail("database", "PostgreSQL")
                .withDetail("version", version)
                .withDetail("queryTime", queryTime + "ms")
                .withDetail("totalOrders", dbStats.get("total_orders"))
                .withDetail("totalEvents", dbStats.get("total_events"))
                .withDetail("totalSagas", dbStats.get("total_sagas"))
                .withDetail("status", "Connected and responsive");
            
            // Add performance warnings
            if (queryTime > 1000) {
                healthBuilder = healthBuilder.withDetail("warning", "Slow query performance detected");
            }
            
            if (queryTime > 5000) {
                return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("error", "Database query timeout")
                    .withDetail("queryTime", queryTime + "ms")
                    .build();
            }
            
            logger.debug("Database health check completed successfully in {}ms", queryTime);
            return healthBuilder.build();
            
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage())
                .withDetail("status", "Connection failed")
                .build();
        }
    }
}
