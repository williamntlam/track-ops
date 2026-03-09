package com.trackops.eventrelay.config;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
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
}