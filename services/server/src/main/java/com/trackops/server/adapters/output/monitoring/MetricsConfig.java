package com.trackops.server.adapters.output.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Configuration for Prometheus metrics in TrackOps application.
 * Defines custom metrics for business and technical monitoring.
 */
@Configuration
public class MetricsConfig {
    
    private final MeterRegistry meterRegistry;
    
    @Autowired
    public MetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    // ==================== ORDER METRICS ====================
    
    @Bean
    public Counter ordersCreatedCounter() {
        return Counter.builder("orders_created_total")
                .description("Total number of orders created")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter ordersStatusUpdatedCounter() {
        return Counter.builder("orders_status_updated_total")
                .description("Total number of order status updates")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter ordersCancelledCounter() {
        return Counter.builder("orders_cancelled_total")
                .description("Total number of orders cancelled")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Timer orderProcessingTimer() {
        return Timer.builder("order_processing_duration_seconds")
                .description("Time taken to process orders")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter ordersByStatusCounter() {
        return Counter.builder("orders_by_status_total")
                .description("Orders grouped by status")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    // ==================== CACHE METRICS ====================
    
    @Bean
    public Counter cacheHitsCounter() {
        return Counter.builder("cache_hits_total")
                .description("Total number of cache hits")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter cacheMissesCounter() {
        return Counter.builder("cache_misses_total")
                .description("Total number of cache misses")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter cacheEvictionsCounter() {
        return Counter.builder("cache_evictions_total")
                .description("Total number of cache evictions")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Timer cacheOperationTimer() {
        return Timer.builder("cache_operation_duration_seconds")
                .description("Time taken for cache operations")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    // ==================== KAFKA METRICS ====================
    
    @Bean
    public Counter kafkaMessagesPublishedCounter() {
        return Counter.builder("kafka_messages_published_total")
                .description("Total number of Kafka messages published")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter kafkaMessagesConsumedCounter() {
        return Counter.builder("kafka_messages_consumed_total")
                .description("Total number of Kafka messages consumed")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter kafkaPublishErrorsCounter() {
        return Counter.builder("kafka_publish_errors_total")
                .description("Total number of Kafka publish errors")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Timer kafkaPublishTimer() {
        return Timer.builder("kafka_publish_duration_seconds")
                .description("Time taken to publish Kafka messages")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    // ==================== SAGA METRICS ====================
    
    @Bean
    public Counter sagaInstancesCreatedCounter() {
        return Counter.builder("saga_instances_created_total")
                .description("Total number of SAGA instances created")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter sagaInstancesCompletedCounter() {
        return Counter.builder("saga_instances_completed_total")
                .description("Total number of SAGA instances completed")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter sagaInstancesFailedCounter() {
        return Counter.builder("saga_instances_failed_total")
                .description("Total number of SAGA instances failed")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Timer sagaExecutionTimer() {
        return Timer.builder("saga_execution_duration_seconds")
                .description("Time taken to execute SAGA instances")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    // ==================== API METRICS ====================
    
    @Bean
    public Counter apiRequestsCounter() {
        return Counter.builder("api_requests_total")
                .description("Total number of API requests")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter apiErrorsCounter() {
        return Counter.builder("api_errors_total")
                .description("Total number of API errors")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    @Bean
    public Timer apiResponseTimer() {
        return Timer.builder("api_response_duration_seconds")
                .description("API response time")
                .tag("application", "trackops-server")
                .register(meterRegistry);
    }
    
    // ==================== BUSINESS METRICS ====================
    
    @Bean
    public AtomicLong activeOrdersGauge() {
        AtomicLong activeOrders = new AtomicLong(0);
        Gauge.builder("active_orders_count", activeOrders, AtomicLong::get)
                .description("Number of active orders")
                .tag("application", "trackops-server")
                .register(meterRegistry);
        return activeOrders;
    }
    
    @Bean
    public AtomicLong totalRevenueGauge() {
        AtomicLong totalRevenue = new AtomicLong(0);
        Gauge.builder("total_revenue_cents", totalRevenue, AtomicLong::get)
                .description("Total revenue in cents")
                .tag("application", "trackops-server")
                .register(meterRegistry);
        return totalRevenue;
    }
    
    @Bean
    public AtomicLong averageOrderValueGauge() {
        AtomicLong averageOrderValue = new AtomicLong(0);
        Gauge.builder("average_order_value_cents", averageOrderValue, AtomicLong::get)
                .description("Average order value in cents")
                .tag("application", "trackops-server")
                .register(meterRegistry);
        return averageOrderValue;
    }
}
