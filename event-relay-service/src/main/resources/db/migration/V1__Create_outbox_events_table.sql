-- Create outbox_events table for event relay service
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    error_message VARCHAR(1000),
    partition_key VARCHAR(255),
    version BIGINT DEFAULT 0
);

-- Create indexes for performance
CREATE INDEX idx_outbox_events_aggregate_id ON outbox_events(aggregate_id);
CREATE INDEX idx_outbox_events_event_type ON outbox_events(event_type);
CREATE INDEX idx_outbox_events_processed ON outbox_events(processed);
CREATE INDEX idx_outbox_events_created_at ON outbox_events(created_at);
CREATE INDEX idx_outbox_events_retry_count ON outbox_events(retry_count);

-- Create composite indexes for common queries
CREATE INDEX idx_outbox_events_unprocessed ON outbox_events(created_at) WHERE processed = FALSE;
CREATE INDEX idx_outbox_events_retryable ON outbox_events(created_at) WHERE processed = FALSE AND retry_count < max_retries;
CREATE INDEX idx_outbox_events_processed_before ON outbox_events(processed_at) WHERE processed = TRUE;

-- Add comments for documentation
COMMENT ON TABLE outbox_events IS 'Outbox pattern events for reliable event publishing';
COMMENT ON COLUMN outbox_events.aggregate_id IS 'ID of the aggregate that generated the event (e.g., orderId)';
COMMENT ON COLUMN outbox_events.event_type IS 'Type of event (e.g., ORDER_CREATED, ORDER_STATUS_UPDATED)';
COMMENT ON COLUMN outbox_events.payload IS 'JSON serialized event data';
COMMENT ON COLUMN outbox_events.processed IS 'Whether the event has been successfully published to Kafka';
COMMENT ON COLUMN outbox_events.retry_count IS 'Number of times this event has been retried';
COMMENT ON COLUMN outbox_events.max_retries IS 'Maximum number of retry attempts before giving up';
COMMENT ON COLUMN outbox_events.partition_key IS 'Kafka partition key for event ordering';
