package com.trackops.eventrelay.config;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.springframework.stereotype.Service;

import java.io.IOException;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for managing Schema Registry operations with local caching.
 * Provides methods to register and retrieve schemas with error handling.
 */
@Slf4j
@Service
public class SchemaRegistryService {

    private final SchemaRegistryClient schemaRegistryClient;
    private final ConcurrentMap<String, Schema> localSchemaCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> subjectToSchemaIdCache = new ConcurrentHashMap<>();

    public SchemaRegistryService(SchemaRegistryClient schemaRegistryClient) {
        this.schemaRegistryClient = schemaRegistryClient;
    }

    /**
     * Registers a schema for the given subject (topic).
     * Returns the schema ID if successful.
     */
    public int registerSchema(String subject, Schema schema) {
        try {
            // Check local cache first
            if (localSchemaCache.containsKey(subject)) {
                Schema cachedSchema = localSchemaCache.get(subject);
                if (cachedSchema.toString().equals(schema.toString())) {
                    Integer cachedId = subjectToSchemaIdCache.get(subject);
                    if (cachedId != null) {
                        log.debug("Using cached schema ID {} for subject {}", cachedId, subject);
                        return cachedId;
                    }
                }
            }

            // Try to get existing schema ID first
            try {
                int existingId = schemaRegistryClient.getId(subject, schema);
                log.debug("Schema already exists for subject {} with ID: {}", subject, existingId);
                
                // Cache the result
                localSchemaCache.put(subject, schema);
                subjectToSchemaIdCache.put(subject, existingId);
                
                return existingId;
            } catch (RestClientException e) {
                if (e.getStatus() == 404) {
                    // Schema doesn't exist, register it
                    int schemaId = schemaRegistryClient.register(subject, schema);
                    log.info("Registered new schema for subject {} with ID: {}", subject, schemaId);
                    
                    // Cache the result
                    localSchemaCache.put(subject, schema);
                    subjectToSchemaIdCache.put(subject, schemaId);
                    
                    return schemaId;
                } else {
                    throw new SchemaRegistryException(
                        "Failed to check/register schema for subject: " + subject, e);
                }
            }
        } catch (RestClientException | IOException e) {
            log.error("Error registering schema for subject {}: {}", subject, e.getMessage(), e);
            throw new SchemaRegistryException("Failed to register schema for subject: " + subject, e);
        }
    }

    /**
     * Retrieves the latest schema for a subject.
     */
    public Schema getLatestSchema(String subject) {
        try {
            String cacheKey = subject + "-latest";
            
            // Check local cache
            if (localSchemaCache.containsKey(cacheKey)) {
                return localSchemaCache.get(cacheKey);
            }

            String schemaString = schemaRegistryClient.getLatestSchemaMetadata(subject).getSchema();
            Schema schema = new org.apache.avro.Schema.Parser().parse(schemaString);

            // Cache the result
            localSchemaCache.put(cacheKey, schema);
            
            return schema;
        } catch (RestClientException | IOException e) {
            log.error("Error retrieving schema for subject {}: {}", subject, e.getMessage(), e);
            throw new SchemaRegistryException(
                "Failed to retrieve schema for subject: " + subject, e);
        }
    }

    /**
     * Custom exception for Schema Registry operations.
     */
    public static class SchemaRegistryException extends RuntimeException {
        public SchemaRegistryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
