-- Create processed_events table for idempotency tracking
CREATE TABLE processed_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) NOT NULL UNIQUE,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    aggregate_status VARCHAR(50),
    processor_name VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create indexes for performance
CREATE INDEX idx_processed_events_event_id ON processed_events(event_id);
CREATE INDEX idx_processed_events_aggregate_id ON processed_events(aggregate_id);
CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);
CREATE INDEX idx_processed_events_processor_name ON processed_events(processor_name);

-- Create composite indexes for common queries
CREATE INDEX idx_processed_events_aggregate_type ON processed_events(aggregate_id, event_type);
CREATE INDEX idx_processed_events_processor_processed ON processed_events(processor_name, processed_at);

-- Add comments for documentation
COMMENT ON TABLE processed_events IS 'Track processed events for idempotency and duplicate prevention';
COMMENT ON COLUMN processed_events.event_id IS 'Unique identifier for the processed event';
COMMENT ON COLUMN processed_events.aggregate_id IS 'ID of the aggregate that generated the event';
COMMENT ON COLUMN processed_events.processor_name IS 'Name of the service that processed the event';
