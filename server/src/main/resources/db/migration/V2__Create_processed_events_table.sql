-- Add additional indexes for processed_events table (table already created in V1)
-- These indexes complement the ones created in V1

-- Create additional composite indexes for common queries
CREATE INDEX idx_processed_events_processor_processed ON processed_events(processor_name, processed_at);

-- Add additional comments for documentation
COMMENT ON COLUMN processed_events.processor_name IS 'Name of the service that processed the event';
