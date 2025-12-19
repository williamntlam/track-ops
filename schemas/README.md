# Schema Registry Schemas

This directory contains Avro schemas for all events published to Kafka topics.

## Schema Compatibility

The default compatibility level is set to **BACKWARD**, which means:
- New schema versions can add optional fields (with defaults)
- New schema versions cannot remove required fields
- Consumers using older schemas can still read messages produced with newer schemas

## Schema Files

### Order Events
- `OrderCreatedEvent.avsc` - Published to `ORDER_CREATED` topic
- `OrderStatusUpdatedEvent.avsc` - Published to `ORDER_STATUS_UPDATED` topic
- `OrderDeliveredEvent.avsc` - Published to `ORDER_DELIVERED` topic
- `OrderCancelledEvent.avsc` - Published to `ORDER_CANCELLED` topic

### Inventory Events
- `InventoryReservedEvent.avsc` - Published to `INVENTORY_RESERVED` topic
- `InventoryReservationFailedEvent.avsc` - Published to `INVENTORY_RESERVATION_FAILED` topic
- `InventoryReleasedEvent.avsc` - Published to `INVENTORY_RELEASED` topic

## Schema Subject Naming

Schemas are registered in the Schema Registry using the following naming convention:
- `<topic-name>-value` for value schemas
- `<topic-name>-key` for key schemas (if needed)

For example:
- `ORDER_CREATED-value`
- `INVENTORY_RESERVED-value`

## Version Control

All schemas are version-controlled in this directory. When making schema changes:

1. Update the schema file in this directory
2. Ensure backward compatibility (add optional fields with defaults)
3. Test schema evolution locally
4. Deploy the new schema version
5. The Schema Registry will automatically validate compatibility

## Schema Evolution Best Practices

1. **Add fields**: Always add new fields as optional with default values
2. **Remove fields**: Mark fields as deprecated first, then remove in a later version
3. **Change types**: Avoid changing field types; create new fields instead
4. **Rename fields**: Use aliases to maintain backward compatibility
