package com.trackops.server.adapters.output.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.time.Duration;

/**
 * Custom health indicator for application-level health metrics.
 * Provides information about JVM health, memory usage, uptime, and application status.
 */
@Component
public class ApplicationHealthIndicator implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationHealthIndicator.class);
    
    @Value("${spring.application.name:trackops-server}")
    private String applicationName;
    
    @Value("${spring.profiles.active:#{null}}")
    private String activeProfile;
    
    private final Instant startTime = Instant.now();
    
    @Override
    public Health health() {
        try {
            // Get JVM information
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            // Calculate uptime
            long uptimeMillis = runtimeBean.getUptime();
            Duration uptime = Duration.ofMillis(uptimeMillis);
            
            // Get memory information
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            
            // Calculate memory usage percentage
            double heapUsagePercent = (double) heapUsed / heapMax * 100;
            
            // Get system information
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long maxMemory = Runtime.getRuntime().maxMemory();
            
            String version = getClass().getPackage().getImplementationVersion();
            if (version == null) {
                version = "1.0.0"; // Default version if not available
            }
            
            String profile = (activeProfile != null) ? activeProfile : "default";
            
            Health.Builder healthBuilder = Health.up()
                .withDetail("application", applicationName)
                .withDetail("profile", profile)
                .withDetail("version", version)
                .withDetail("uptime", formatDuration(uptime))
                .withDetail("uptimeMillis", uptimeMillis)
                .withDetail("jvm", runtimeBean.getVmName() + " " + runtimeBean.getVmVersion())
                .withDetail("javaVersion", System.getProperty("java.version"))
                .withDetail("availableProcessors", availableProcessors)
                .withDetail("heapUsed", formatBytes(heapUsed))
                .withDetail("heapMax", formatBytes(heapMax))
                .withDetail("heapUsagePercent", String.format("%.2f%%", heapUsagePercent))
                .withDetail("nonHeapUsed", formatBytes(nonHeapUsed))
                .withDetail("totalMemory", formatBytes(totalMemory))
                .withDetail("freeMemory", formatBytes(freeMemory))
                .withDetail("maxMemory", formatBytes(maxMemory))
                .withDetail("status", "Application is running normally");
            
            // Add warnings for high memory usage
            if (heapUsagePercent > 80) {
                healthBuilder = healthBuilder.withDetail("warning", "High memory usage detected");
            }
            
            if (heapUsagePercent > 95) {
                return Health.down()
                    .withDetail("application", applicationName)
                    .withDetail("error", "Critical memory usage")
                    .withDetail("heapUsagePercent", String.format("%.2f%%", heapUsagePercent))
                    .build();
            }
            
            // Add warnings for long uptime (potential memory leaks)
            if (uptime.toDays() > 30) {
                healthBuilder = healthBuilder.withDetail("info", "Application has been running for over 30 days");
            }
            
            logger.debug("Application health check completed successfully");
            return healthBuilder.build();
            
        } catch (Exception e) {
            logger.error("Application health check failed", e);
            return Health.down()
                .withDetail("application", applicationName)
                .withDetail("error", e.getMessage())
                .withDetail("status", "Health check failed")
                .build();
        }
    }
    
    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
