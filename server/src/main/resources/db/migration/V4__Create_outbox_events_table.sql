-- Create outbox events table
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    error_message VARCHAR(1000),
    partition_key VARCHAR(255),
    version BIGINT DEFAULT 0
);

-- Create indexes for performance
CREATE INDEX idx_outbox_events_processed ON outbox_events(processed, created_at);
CREATE INDEX idx_outbox_events_aggregate_id ON outbox_events(aggregate_id);
CREATE INDEX idx_outbox_events_event_type ON outbox_events(event_type);
CREATE INDEX idx_outbox_events_created_at ON outbox_events(created_at);
CREATE INDEX idx_outbox_events_retry_count ON outbox_events(retry_count);

-- Create composite indexes for common queries
CREATE INDEX idx_outbox_events_unprocessed ON outbox_events(processed, retry_count, created_at);
CREATE INDEX idx_outbox_events_cleanup ON outbox_events(processed, processed_at);

-- Add comments for documentation
COMMENT ON TABLE outbox_events IS 'Outbox pattern table for reliable event publishing';
COMMENT ON COLUMN outbox_events.aggregate_id IS 'ID of the aggregate (e.g., order ID)';
COMMENT ON COLUMN outbox_events.event_type IS 'Type of event (e.g., ORDER_CREATED)';
COMMENT ON COLUMN outbox_events.payload IS 'JSON serialized event data';
COMMENT ON COLUMN outbox_events.processed IS 'Whether the event has been published to Kafka';
COMMENT ON COLUMN outbox_events.retry_count IS 'Number of times publishing has been attempted';
COMMENT ON COLUMN outbox_events.partition_key IS 'Kafka partition key for ordering';
