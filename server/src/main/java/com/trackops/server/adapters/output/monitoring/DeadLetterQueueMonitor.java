package com.trackops.server.adapters.output.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitor for Dead Letter Queue messages.
 * Tracks failed messages and provides metrics for monitoring.
 * Only active when Debezium strategy is enabled.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.event-publishing.strategy", havingValue = "debezium")
public class DeadLetterQueueMonitor {
    
    private final AtomicLong debeziumOrderEventDlqCount = new AtomicLong(0);
    private final AtomicLong debeziumCacheConsumerDlqCount = new AtomicLong(0);
    private final AtomicLong debeziumCacheWarmerDlqCount = new AtomicLong(0);
    
    /**
     * Monitor Debezium Order Event DLQ
     */
    @KafkaListener(
        topics = "debezium-order-event-dlq",
        groupId = "dlq-monitor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDebeziumOrderEventDlq(
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            String payload) {
        
        long count = debeziumOrderEventDlqCount.incrementAndGet();
        log.error("🚨 DLQ MESSAGE RECEIVED - Debezium Order Event DLQ");
        log.error("Topic: {}, Partition: {}, Offset: {}", topic, partition, offset);
        log.error("Payload: {}", payload);
        log.error("Total DLQ messages for Debezium Order Event: {}", count);
        log.error("Timestamp: {}", LocalDateTime.now());
        
        // TODO: Send alert to monitoring system (e.g., Slack, PagerDuty)
        // TODO: Store in database for analysis
    }
    
    /**
     * Monitor Debezium Cache Consumer DLQ
     */
    @KafkaListener(
        topics = "debezium-cache-consumer-dlq",
        groupId = "dlq-monitor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDebeziumCacheConsumerDlq(
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            String payload) {
        
        long count = debeziumCacheConsumerDlqCount.incrementAndGet();
        log.error("🚨 DLQ MESSAGE RECEIVED - Debezium Cache Consumer DLQ");
        log.error("Topic: {}, Partition: {}, Offset: {}", topic, partition, offset);
        log.error("Payload: {}", payload);
        log.error("Total DLQ messages for Debezium Cache Consumer: {}", count);
        log.error("Timestamp: {}", LocalDateTime.now());
    }
    
    /**
     * Monitor Debezium Cache Warmer DLQ
     */
    @KafkaListener(
        topics = "debezium-cache-warmer-dlq",
        groupId = "dlq-monitor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDebeziumCacheWarmerDlq(
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            String payload) {
        
        long count = debeziumCacheWarmerDlqCount.incrementAndGet();
        log.error("🚨 DLQ MESSAGE RECEIVED - Debezium Cache Warmer DLQ");
        log.error("Topic: {}, Partition: {}, Offset: {}", topic, partition, offset);
        log.error("Payload: {}", payload);
        log.error("Total DLQ messages for Debezium Cache Warmer: {}", count);
        log.error("Timestamp: {}", LocalDateTime.now());
    }
    
    /**
     * Get DLQ metrics for monitoring
     */
    public DLQMetrics getMetrics() {
        return DLQMetrics.builder()
            .debeziumOrderEventDlqCount(debeziumOrderEventDlqCount.get())
            .debeziumCacheConsumerDlqCount(debeziumCacheConsumerDlqCount.get())
            .debeziumCacheWarmerDlqCount(debeziumCacheWarmerDlqCount.get())
            .totalDlqCount(debeziumOrderEventDlqCount.get() + 
                          debeziumCacheConsumerDlqCount.get() + 
                          debeziumCacheWarmerDlqCount.get())
            .build();
    }
    
    /**
     * DLQ Metrics data class
     */
    public static class DLQMetrics {
        private final long debeziumOrderEventDlqCount;
        private final long debeziumCacheConsumerDlqCount;
        private final long debeziumCacheWarmerDlqCount;
        private final long totalDlqCount;
        
        private DLQMetrics(Builder builder) {
            this.debeziumOrderEventDlqCount = builder.debeziumOrderEventDlqCount;
            this.debeziumCacheConsumerDlqCount = builder.debeziumCacheConsumerDlqCount;
            this.debeziumCacheWarmerDlqCount = builder.debeziumCacheWarmerDlqCount;
            this.totalDlqCount = builder.totalDlqCount;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private long debeziumOrderEventDlqCount;
            private long debeziumCacheConsumerDlqCount;
            private long debeziumCacheWarmerDlqCount;
            private long totalDlqCount;
            
            public Builder debeziumOrderEventDlqCount(long count) {
                this.debeziumOrderEventDlqCount = count;
                return this;
            }
            
            public Builder debeziumCacheConsumerDlqCount(long count) {
                this.debeziumCacheConsumerDlqCount = count;
                return this;
            }
            
            public Builder debeziumCacheWarmerDlqCount(long count) {
                this.debeziumCacheWarmerDlqCount = count;
                return this;
            }
            
            public Builder totalDlqCount(long count) {
                this.totalDlqCount = count;
                return this;
            }
            
            public DLQMetrics build() {
                return new DLQMetrics(this);
            }
        }
        
        // Getters
        public long getDebeziumOrderEventDlqCount() { return debeziumOrderEventDlqCount; }
        public long getDebeziumCacheConsumerDlqCount() { return debeziumCacheConsumerDlqCount; }
        public long getDebeziumCacheWarmerDlqCount() { return debeziumCacheWarmerDlqCount; }
        public long getTotalDlqCount() { return totalDlqCount; }
    }
}
