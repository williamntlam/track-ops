package com.trackops.inventory.config;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Confluent Schema Registry integration.
 * Provides schema registry client with caching and error handling.
 */
@Slf4j
@Configuration
public class SchemaRegistryConfig {

    @Value("${schema.registry.url:http://localhost:8081}")
    private String schemaRegistryUrl;

    @Value("${schema.registry.basic.auth.user.info:}")
    private String basicAuthUserInfo;

    @Value("${schema.registry.cache.capacity:1000}")
    private int cacheCapacity;

    @Value("${schema.registry.auto.register.schemas:true}")
    private boolean autoRegisterSchemas;

    @Value("${schema.registry.compatibility.level:BACKWARD}")
    private String compatibilityLevel;

    /**
     * Creates a SchemaRegistryClient with caching and authentication support.
     * The client caches schemas locally to reduce latency.
     */
    @Bean
    public SchemaRegistryClient schemaRegistryClient() {
        Map<String, Object> config = new HashMap<>();
        
        // Basic authentication if provided
        if (basicAuthUserInfo != null && !basicAuthUserInfo.isEmpty()) {
            config.put("basic.auth.credentials.source", "USER_INFO");
            config.put("basic.auth.user.info", basicAuthUserInfo);
            log.info("Schema Registry authentication configured");
        }
        
        // Cache configuration
        config.put("schema.registry.cache.capacity", cacheCapacity);
        
        SchemaRegistryClient client = new CachedSchemaRegistryClient(
            Collections.singletonList(schemaRegistryUrl),
            cacheCapacity,
            config
        );
        
        log.info("Schema Registry client initialized with URL: {}", schemaRegistryUrl);
        
        // Set compatibility level if auto-register is enabled
        if (autoRegisterSchemas) {
            try {
                setCompatibilityLevel(client);
            } catch (Exception e) {
                log.warn("Failed to set compatibility level: {}", e.getMessage());
            }
        }
        
        return client;
    }

    /**
     * Sets the default compatibility level for the Schema Registry.
     */
    private void setCompatibilityLevel(SchemaRegistryClient client) {
        try {
            // Set global compatibility level
            client.updateCompatibility("", compatibilityLevel);
            log.info("Schema Registry compatibility level set to: {}", compatibilityLevel);
        } catch (RestClientException | IOException e) {
            log.warn("Could not set global compatibility level (may not have permissions): {}", e.getMessage());
        }
    }

    /**
     * Registers schemas from the schemas directory if auto-register is enabled.
     * This is useful for development environments.
     */
    @Bean
    public SchemaRegistryInitializer schemaRegistryInitializer(SchemaRegistryClient schemaRegistryClient) {
        return new SchemaRegistryInitializer(schemaRegistryClient, autoRegisterSchemas);
    }

    /**
     * Helper class to initialize schemas on startup.
     */
    public static class SchemaRegistryInitializer {
        private final SchemaRegistryClient client;
        private final boolean autoRegister;

        public SchemaRegistryInitializer(SchemaRegistryClient client, boolean autoRegister) {
            this.client = client;
            this.autoRegister = autoRegister;
            
            if (autoRegister) {
                initializeSchemas();
            }
        }

        private void initializeSchemas() {
            try {
                Path schemasDir = Paths.get("schemas/avro");
                if (!Files.exists(schemasDir)) {
                    log.warn("Schemas directory not found: {}. Skipping auto-registration.", schemasDir);
                    return;
                }

                log.info("Auto-registering schemas from: {}", schemasDir);
                Files.list(schemasDir)
                    .filter(path -> path.toString().endsWith(".avsc"))
                    .forEach(this::registerSchema);
            } catch (IOException e) {
                log.error("Error reading schemas directory", e);
            }
        }

        private void registerSchema(Path schemaPath) {
            try {
                String schemaContent = Files.readString(schemaPath);
                Schema schema = new Schema.Parser().parse(schemaContent);
                
                // Extract subject name from filename (e.g., InventoryReservedEvent.avsc -> INVENTORY_RESERVED-value)
                String fileName = schemaPath.getFileName().toString();
                String eventName = fileName.replace(".avsc", "");
                String subject = getSubjectForEvent(eventName);
                
                try {
                    // Check if schema already exists
                    int existingSchemaId = client.getId(subject, schema);
                    log.debug("Schema already registered for subject {} with ID: {}", subject, existingSchemaId);
                } catch (RestClientException e) {
                    if (e.getStatus() == 404) {
                        // Schema doesn't exist, register it
                        int schemaId = client.register(subject, schema);
                        log.info("Registered schema for subject {} with ID: {}", subject, schemaId);
                    } else {
                        log.error("Error checking/registering schema for subject {}: {}", subject, e.getMessage());
                    }
                }
            } catch (IOException | RestClientException e) {
                log.error("Error registering schema from file {}: {}", schemaPath, e.getMessage());
            }
        }

        private String getSubjectForEvent(String eventName) {
            // Map event class names to topic names
            Map<String, String> eventToTopic = Map.of(
                "OrderCreatedEvent", "ORDER_CREATED",
                "OrderStatusUpdatedEvent", "ORDER_STATUS_UPDATED",
                "OrderDeliveredEvent", "ORDER_DELIVERED",
                "OrderCancelledEvent", "ORDER_CANCELLED",
                "InventoryReservedEvent", "INVENTORY_RESERVED",
                "InventoryReservationFailedEvent", "INVENTORY_RESERVATION_FAILED",
                "InventoryReleasedEvent", "INVENTORY_RELEASED"
            );
            
            String topic = eventToTopic.get(eventName);
            return topic != null ? topic + "-value" : eventName + "-value";
        }
    }
}
