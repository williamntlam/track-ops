package com.trackops.server.adapters.output.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer.Sample;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

/**
 * Service for recording custom metrics in TrackOps application.
 * Provides a centralized way to record business and technical metrics.
 */
@Service
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Counter ordersCreatedCounter;
    private final Counter ordersStatusUpdatedCounter;
    private final Counter ordersCancelledCounter;
    private final Timer orderProcessingTimer;
    private final Counter ordersByStatusCounter;
    
    private final Counter cacheHitsCounter;
    private final Counter cacheMissesCounter;
    private final Counter cacheEvictionsCounter;
    private final Timer cacheOperationTimer;
    
    private final Counter kafkaMessagesPublishedCounter;
    private final Counter kafkaMessagesConsumedCounter;
    private final Counter kafkaPublishErrorsCounter;
    private final Timer kafkaPublishTimer;
    
    private final Counter sagaInstancesCreatedCounter;
    private final Counter sagaInstancesCompletedCounter;
    private final Counter sagaInstancesFailedCounter;
    private final Timer sagaExecutionTimer;
    
    private final Counter apiRequestsCounter;
    private final Counter apiErrorsCounter;
    private final Timer apiResponseTimer;
    
    private final AtomicLong activeOrdersGauge;
    private final AtomicLong totalRevenueGauge;
    private final AtomicLong averageOrderValueGauge;
    
    @Autowired
    public MetricsService(
            MeterRegistry meterRegistry,
            Counter ordersCreatedCounter,
            Counter ordersStatusUpdatedCounter,
            Counter ordersCancelledCounter,
            Timer orderProcessingTimer,
            Counter ordersByStatusCounter,
            Counter cacheHitsCounter,
            Counter cacheMissesCounter,
            Counter cacheEvictionsCounter,
            Timer cacheOperationTimer,
            Counter kafkaMessagesPublishedCounter,
            Counter kafkaMessagesConsumedCounter,
            Counter kafkaPublishErrorsCounter,
            Timer kafkaPublishTimer,
            Counter sagaInstancesCreatedCounter,
            Counter sagaInstancesCompletedCounter,
            Counter sagaInstancesFailedCounter,
            Timer sagaExecutionTimer,
            Counter apiRequestsCounter,
            Counter apiErrorsCounter,
            Timer apiResponseTimer,
            AtomicLong activeOrdersGauge,
            AtomicLong totalRevenueGauge,
            AtomicLong averageOrderValueGauge) {
        
        this.meterRegistry = meterRegistry;
        this.ordersCreatedCounter = ordersCreatedCounter;
        this.ordersStatusUpdatedCounter = ordersStatusUpdatedCounter;
        this.ordersCancelledCounter = ordersCancelledCounter;
        this.orderProcessingTimer = orderProcessingTimer;
        this.ordersByStatusCounter = ordersByStatusCounter;
        this.cacheHitsCounter = cacheHitsCounter;
        this.cacheMissesCounter = cacheMissesCounter;
        this.cacheEvictionsCounter = cacheEvictionsCounter;
        this.cacheOperationTimer = cacheOperationTimer;
        this.kafkaMessagesPublishedCounter = kafkaMessagesPublishedCounter;
        this.kafkaMessagesConsumedCounter = kafkaMessagesConsumedCounter;
        this.kafkaPublishErrorsCounter = kafkaPublishErrorsCounter;
        this.kafkaPublishTimer = kafkaPublishTimer;
        this.sagaInstancesCreatedCounter = sagaInstancesCreatedCounter;
        this.sagaInstancesCompletedCounter = sagaInstancesCompletedCounter;
        this.sagaInstancesFailedCounter = sagaInstancesFailedCounter;
        this.sagaExecutionTimer = sagaExecutionTimer;
        this.apiRequestsCounter = apiRequestsCounter;
        this.apiErrorsCounter = apiErrorsCounter;
        this.apiResponseTimer = apiResponseTimer;
        this.activeOrdersGauge = activeOrdersGauge;
        this.totalRevenueGauge = totalRevenueGauge;
        this.averageOrderValueGauge = averageOrderValueGauge;
    }
    
    // ==================== ORDER METRICS ====================
    
    public void recordOrderCreated() {
        ordersCreatedCounter.increment();
    }
    
    public void recordOrderStatusUpdated(String status) {
        ordersStatusUpdatedCounter.increment();
        ordersByStatusCounter.increment();
    }
    
    public void recordOrderCancelled() {
        ordersCancelledCounter.increment();
    }
    
    public Timer.Sample startOrderProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordOrderProcessingTime(Timer.Sample sample) {
        sample.stop(orderProcessingTimer);
    }
    
    // ==================== CACHE METRICS ====================
    
    public void recordCacheHit(String cacheName) {
        cacheHitsCounter.increment();
    }
    
    public void recordCacheMiss(String cacheName) {
        cacheMissesCounter.increment();
    }
    
    public void recordCacheEviction(String cacheName) {
        cacheEvictionsCounter.increment();
    }
    
    public Timer.Sample startCacheOperationTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordCacheOperationTime(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("cache_operation_duration_seconds")
                .tag("operation", operation)
                .register(meterRegistry));
    }
    
    // ==================== KAFKA METRICS ====================
    
    public void recordKafkaMessagePublished(String topic) {
        kafkaMessagesPublishedCounter.increment();
    }
    
    public void recordKafkaMessageConsumed(String topic) {
        kafkaMessagesConsumedCounter.increment();
    }
    
    public void recordKafkaPublishError(String topic, String error) {
        kafkaPublishErrorsCounter.increment();
    }
    
    public Timer.Sample startKafkaPublishTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordKafkaPublishTime(Timer.Sample sample, String topic) {
        sample.stop(Timer.builder("kafka_publish_duration_seconds")
                .tag("topic", topic)
                .register(meterRegistry));
    }
    
    // ==================== SAGA METRICS ====================
    
    public void recordSagaInstanceCreated(String sagaType) {
        sagaInstancesCreatedCounter.increment();
    }
    
    public void recordSagaInstanceCompleted(String sagaType) {
        sagaInstancesCompletedCounter.increment();
    }
    
    public void recordSagaInstanceFailed(String sagaType, String error) {
        sagaInstancesFailedCounter.increment();
    }
    
    public Timer.Sample startSagaExecutionTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordSagaExecutionTime(Timer.Sample sample, String sagaType) {
        sample.stop(Timer.builder("saga_execution_duration_seconds")
                .tag("saga_type", sagaType)
                .register(meterRegistry));
    }
    
    // ==================== API METRICS ====================
    
    public void recordApiRequest(String method, String endpoint) {
        apiRequestsCounter.increment();
    }
    
    public void recordApiError(String method, String endpoint, String error) {
        apiErrorsCounter.increment();
    }
    
    public Timer.Sample startApiResponseTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordApiResponseTime(Timer.Sample sample, String method, String endpoint) {
        sample.stop(Timer.builder("api_response_duration_seconds")
                .tag("method", method)
                .tag("endpoint", endpoint)
                .register(meterRegistry));
    }
    
    // ==================== BUSINESS METRICS ====================
    
    public void updateActiveOrdersCount(long count) {
        activeOrdersGauge.set(count);
    }
    
    public void updateTotalRevenue(long revenueCents) {
        totalRevenueGauge.set(revenueCents);
    }
    
    public long getTotalRevenue() {
        return totalRevenueGauge.get();
    }
    
    public void updateAverageOrderValue(long averageValueCents) {
        averageOrderValueGauge.set(averageValueCents);
    }
    
    // ==================== UTILITY METHODS ====================
    
    public void recordExecutionTime(Runnable operation, String operationName) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            operation.run();
        } finally {
            sample.stop(Timer.builder("operation_duration_seconds")
                    .tag("operation", operationName)
                    .register(meterRegistry));
        }
    }
    
    public <T> T recordExecutionTime(java.util.function.Supplier<T> operation, String operationName) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return operation.get();
        } finally {
            sample.stop(Timer.builder("operation_duration_seconds")
                    .tag("operation", operationName)
                    .register(meterRegistry));
        }
    }
}
