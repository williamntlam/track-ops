package com.trackops.eventrelay.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

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
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<UUID, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Topic Definitions - All Order Service Topics
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
}
