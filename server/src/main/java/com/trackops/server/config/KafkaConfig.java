package com.trackops.server.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:trackops-orders}")
    private String consumerGroupId;

    // Producer Configuration
    @Bean
    public ProducerFactory<UUID, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<UUID, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Consumer Configuration
    @Bean
    public ConsumerFactory<UUID, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // Number of consumer threads
        factory.getContainerProperties().setAckMode(
            org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    // Topic Definitions
    @Bean
    public NewTopic orderCreatedTopic() {
        return new NewTopic("ORDER_CREATED", 3, (short) 1);
    }

    @Bean
    public NewTopic orderStatusUpdatedTopic() {
        return new NewTopic("ORDER_STATUS_UPDATED", 3, (short) 1);
    }

    @Bean
    public NewTopic orderDeliveredTopic() {
        return new NewTopic("ORDER_DELIVERED", 3, (short) 1);
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return new NewTopic("ORDER_CANCELLED", 3, (short) 1);
    }

    // Inventory Service Response Topics
    @Bean
    public NewTopic inventoryReservedTopic() {
        return new NewTopic("INVENTORY_RESERVED", 3, (short) 1);
    }

    @Bean
    public NewTopic inventoryReservationFailedTopic() {
        return new NewTopic("INVENTORY_RESERVATION_FAILED", 3, (short) 1);
    }

    @Bean
    public NewTopic inventoryReleasedTopic() {
        return new NewTopic("INVENTORY_RELEASED", 3, (short) 1);
    }

    // Dead Letter Queue Topics for Debezium Consumers
    @Bean
    public NewTopic debeziumOrderEventDlqTopic() {
        return new NewTopic("debezium-order-event-dlq", 3, (short) 1);
    }

    @Bean
    public NewTopic debeziumCacheConsumerDlqTopic() {
        return new NewTopic("debezium-cache-consumer-dlq", 3, (short) 1);
    }

    @Bean
    public NewTopic debeziumCacheWarmerDlqTopic() {
        return new NewTopic("debezium-cache-warmer-dlq", 3, (short) 1);
    }

    // Dead Letter Queue Configuration
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer() {
        return new DeadLetterPublishingRecoverer(kafkaTemplate());
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Retry policy: 3 attempts with exponential backoff
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000); // 1 second
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000); // 10 seconds
        
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));
        
        return retryTemplate;
    }

    @Bean
    public KafkaListenerErrorHandler debeziumErrorHandler() {
        return new KafkaListenerErrorHandler() {
            @Override
            public Object handleError(Message<?> message, ListenerExecutionFailedException exception) {
                log.error("Debezium consumer error occurred", exception);
                
                // Log the failed message details
                if (message != null) {
                    log.error("Failed message headers: {}", message.getHeaders());
                    log.error("Failed message payload: {}", message.getPayload());
                }
                
                // For now, we'll just log the error and continue
                // In production, you might want to:
                // 1. Send to dead letter queue
                // 2. Retry with exponential backoff
                // 3. Alert monitoring systems
                
                return null;
            }
        };
    }
}