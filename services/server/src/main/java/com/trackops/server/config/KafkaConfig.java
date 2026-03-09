package com.trackops.server.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
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

    @Value("${schema.registry.url:http://localhost:8081}")
    private String schemaRegistryUrl;

    @Value("${schema.registry.basic.auth.user.info:}")
    private String basicAuthUserInfo;

    // Producer Configuration with Avro Serializer
    @Bean
    public ProducerFactory<UUID, GenericRecord> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Schema Registry configuration
        configProps.put("schema.registry.url", schemaRegistryUrl);
        if (basicAuthUserInfo != null && !basicAuthUserInfo.isEmpty()) {
            configProps.put("basic.auth.credentials.source", "USER_INFO");
            configProps.put("basic.auth.user.info", basicAuthUserInfo);
        }
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<UUID, GenericRecord> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Producer Factory for Outbox Event Publisher (UUID key, String value)
    @Bean
    public ProducerFactory<UUID, String> outboxProducerFactory() {
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
    public KafkaTemplate<UUID, String> outboxKafkaTemplate() {
        return new KafkaTemplate<>(outboxProducerFactory());
    }

    // KafkaAdmin for topic management - uses localhost for local development
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Configure client to handle advertised listener issues
        configs.put(org.apache.kafka.clients.CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 30000);
        configs.put(org.apache.kafka.clients.CommonClientConfigs.RECONNECT_BACKOFF_MS_CONFIG, 50);
        configs.put(org.apache.kafka.clients.CommonClientConfigs.RETRY_BACKOFF_MS_CONFIG, 100);
        return new KafkaAdmin(configs);
    }

    // Consumer Configuration with Avro Deserializer
    @Bean
    public ConsumerFactory<UUID, GenericRecord> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        
        // Schema Registry configuration
        configProps.put("schema.registry.url", schemaRegistryUrl);
        configProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false); // Use GenericRecord
        if (basicAuthUserInfo != null && !basicAuthUserInfo.isEmpty()) {
            configProps.put("basic.auth.credentials.source", "USER_INFO");
            configProps.put("basic.auth.user.info", basicAuthUserInfo);
        }
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, GenericRecord> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, GenericRecord> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // Number of consumer threads
        factory.getContainerProperties().setAckMode(
            org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    /** Consumer for CDC topics (Debezium JSON payload). */
    @Bean
    public ConsumerFactory<String, String> cdcStringConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId + "-cdc");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> cdcStringListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cdcStringConsumerFactory());
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
    public NewTopic inventoryReserveRequestTopic() {
        return new NewTopic("INVENTORY_RESERVE_REQUEST", 3, (short) 1);
    }

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