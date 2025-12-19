package com.trackops.inventory.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
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

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:inventory-service}")
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

    // Topic Definitions
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
