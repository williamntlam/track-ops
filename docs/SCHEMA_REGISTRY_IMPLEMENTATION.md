# Confluent Schema Registry Implementation

## Overview

This document describes the implementation of Confluent Schema Registry integration for the TrackOps microservices architecture. The Schema Registry ensures data integrity by enforcing schema validation and compatibility checks, preventing data mismatches that could crash the Kafka pipeline.

## Implementation Summary

### 1. Dependencies Added

All three services (server, inventory-service, event-relay-service) now include:
- `io.confluent:kafka-avro-serializer:7.6.0` - Avro serialization with Schema Registry
- `io.confluent:kafka-schema-registry-client:7.6.0` - Schema Registry client
- `org.apache.avro:avro:1.11.3` - Avro core library

The Confluent Maven repository has been added to all `build.gradle` files.

### 2. Avro Schemas Created

All event schemas are stored in `/schemas/avro/`:
- `OrderCreatedEvent.avsc`
- `OrderStatusUpdatedEvent.avsc`
- `OrderDeliveredEvent.avsc`
- `OrderCancelledEvent.avsc`
- `InventoryReservedEvent.avsc`
- `InventoryReservationFailedEvent.avsc`
- `InventoryReleasedEvent.avsc`

### 3. Schema Registry Service

Added to `docker/kafka.yml`:
- Runs on port 8081
- Configured with BACKWARD compatibility (default)
- Automatically creates `_schemas` topic in Kafka
- Health check included

### 4. Configuration Classes

#### SchemaRegistryConfig
- Creates and configures the Schema Registry client
- Supports basic authentication via environment variables
- Implements local schema caching (default: 1000 schemas)
- Auto-registers schemas on startup (configurable)

#### SchemaRegistryService
- Provides high-level API for schema operations
- Implements local caching to reduce latency
- Handles schema registration and retrieval
- Includes error handling and retry logic

#### AvroEventConverter
- Converts domain event POJOs to Avro GenericRecord (for producers)
- Converts Avro GenericRecord back to POJOs (for consumers)
- Handles all order event types

### 5. Kafka Configuration Updates

Updated `KafkaConfig` in server service:
- **Producers**: Now use `KafkaAvroSerializer` with Schema Registry
- **Consumers**: Now use `KafkaAvroDeserializer` with Schema Registry
- Schema Registry URL and authentication configured via properties

### 6. Producer Updates

`KafkaOrderEventProducer`:
- Converts events to Avro GenericRecord before sending
- Schema Registry automatically validates and registers schemas
- Type mismatches are caught before messages reach Kafka

### 7. Consumer Updates

`KafkaOrderEventConsumer`:
- Receives Avro GenericRecord from Kafka
- Converts back to domain event POJOs
- Schema Registry ensures compatibility with producer schemas

## Configuration

### Environment Variables

The following environment variables can be set:

| Variable | Description | Default |
|----------|-------------|---------|
| `SCHEMA_REGISTRY_URL` | Schema Registry endpoint | `http://localhost:8081` |
| `SCHEMA_REGISTRY_BASIC_AUTH_USER_INFO` | API key:secret for authentication | (empty) |

### Application Properties

In `application.properties`:

```properties
# Schema Registry Configuration
schema.registry.url=http://localhost:8081
schema.registry.basic.auth.user.info=${SCHEMA_REGISTRY_BASIC_AUTH_USER_INFO:}
schema.registry.cache.capacity=1000
schema.registry.auto.register.schemas=true
schema.registry.compatibility.level=BACKWARD
```

## Schema Compatibility

The default compatibility level is **BACKWARD**, which means:
- ✅ New schema versions can add optional fields (with defaults)
- ✅ Consumers using older schemas can read messages with newer schemas
- ❌ New schema versions cannot remove required fields
- ❌ New schema versions cannot change field types

### Compatibility Levels

- **BACKWARD**: New schema must be readable by old consumers (default)
- **FORWARD**: Old schema must be readable by new consumers
- **FULL**: Both backward and forward compatible
- **NONE**: No compatibility checks

## How It Works

### Producer Flow

1. Domain event POJO is created
2. `AvroEventConverter` converts POJO to Avro GenericRecord
3. `KafkaAvroSerializer` sends the record to Schema Registry
4. Schema Registry validates the schema:
   - If schema doesn't exist: registers it
   - If schema exists: validates compatibility
   - If incompatible: throws exception (prevents bad data)
5. Serialized message (with schema ID) is sent to Kafka

### Consumer Flow

1. Consumer receives message from Kafka
2. `KafkaAvroDeserializer` extracts schema ID from message
3. Fetches schema from Schema Registry (cached locally)
4. Deserializes message using the schema
5. `AvroEventConverter` converts GenericRecord to POJO
6. Domain event is processed

## Benefits

1. **Data Integrity**: Schema validation prevents malformed messages
2. **Type Safety**: Type mismatches are caught at serialization time
3. **Schema Evolution**: Controlled schema changes with compatibility checks
4. **Error Prevention**: Incompatible schemas are rejected before reaching Kafka
5. **Versioning**: Automatic schema versioning and management
6. **Performance**: Local schema caching reduces latency

## Testing

### Local Testing

1. Start Schema Registry:
   ```bash
   docker-compose -f docker/kafka.yml up -d
   ```

2. Verify Schema Registry is running:
   ```bash
   curl http://localhost:8081/subjects
   ```

3. Start the application - schemas will be auto-registered

### Schema Compatibility Test

To test compatibility enforcement:

1. Try to register an incompatible schema (e.g., remove a required field)
2. The Schema Registry will return a 409 Conflict error
3. The producer will fail to send the message
4. This prevents bad data from entering the pipeline

## Troubleshooting

### Schema Registry Connection Issues

- Check that Schema Registry is running: `curl http://localhost:8081/subjects`
- Verify the URL in `application.properties`
- Check network connectivity between services

### Schema Registration Failures

- Check Schema Registry logs
- Verify schema files are valid Avro schemas
- Ensure compatibility level allows the change

### Deserialization Errors

- Verify consumer and producer are using compatible schemas
- Check Schema Registry for schema versions
- Review error logs for specific field mismatches

## Next Steps

1. **Inventory Service**: Update to use Avro serialization (similar to server service)
2. **Event Relay Service**: Update to use Avro serialization
3. **Schema Evolution**: Document schema change procedures
4. **Monitoring**: Add metrics for schema registry operations
5. **Testing**: Add integration tests for schema compatibility

## References

- [Confluent Schema Registry Documentation](https://docs.confluent.io/platform/current/schema-registry/index.html)
- [Avro Schema Specification](https://avro.apache.org/docs/current/spec.html)
- [Schema Evolution and Compatibility](https://docs.confluent.io/platform/current/schema-registry/avro.html)
