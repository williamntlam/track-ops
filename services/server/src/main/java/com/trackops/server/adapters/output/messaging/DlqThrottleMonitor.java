package com.trackops.server.adapters.output.messaging;

import com.trackops.server.application.services.dlq.DlqOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Monitors dlq_orders table size and pauses the Debezium order-event consumer when
 * the high watermark is exceeded; resumes when the count falls back to the low watermark.
 * Prevents disk exhaustion from unbounded DLQ growth during downstream outages.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${app.event-publishing.strategy:outbox}' == 'debezium' && ${app.dlq.throttle.enabled:false}")
public class DlqThrottleMonitor {

    static final String DEBEZIUM_ORDER_CONSUMER_ID = "debezium-order-event-consumer";

    private final DlqOrderService dlqOrderService;
    private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    private volatile boolean consumerPausedByThrottle = false;

    @Value("${app.dlq.throttle.high-watermark:50000}")
    private long highWatermark;

    @Value("${app.dlq.throttle.low-watermark:10000}")
    private long lowWatermark;

    /**
     * Check DLQ depth and pause/resume the order-event consumer accordingly.
     * Runs on a fixed delay (default 60s) to avoid over-querying the DB.
     */
    @Scheduled(fixedDelayString = "${app.dlq.throttle.check-interval-ms:60000}")
    public void checkDlqDepthAndThrottle() {
        MessageListenerContainer container = getOrderEventContainer();
        if (container == null) {
            return;
        }

        long count = dlqOrderService.countTotal();

        if (count >= highWatermark) {
            if (container.isRunning()) {
                log.warn("DLQ throttle: count {} >= high watermark {}. Pausing Debezium order-event consumer to protect DB.",
                        count, highWatermark);
                container.stop();
                consumerPausedByThrottle = true;
            }
        } else if (count <= lowWatermark && consumerPausedByThrottle) {
            if (!container.isRunning()) {
                log.info("DLQ throttle: count {} <= low watermark {}. Resuming Debezium order-event consumer.",
                        count, lowWatermark);
                container.start();
                consumerPausedByThrottle = false;
            }
        }
    }

    /** Expose whether the consumer is currently paused due to throttle (for metrics/health). */
    public boolean isConsumerPausedByThrottle() {
        return consumerPausedByThrottle;
    }

    private MessageListenerContainer getOrderEventContainer() {
        try {
            return kafkaListenerEndpointRegistry.getListenerContainer(DEBEZIUM_ORDER_CONSUMER_ID);
        } catch (Exception e) {
            log.trace("Could not get listener container for {}: {}", DEBEZIUM_ORDER_CONSUMER_ID, e.getMessage());
            return null;
        }
    }
}
