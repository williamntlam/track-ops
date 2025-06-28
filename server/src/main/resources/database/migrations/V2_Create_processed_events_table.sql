CREATE TABLE processed_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL UNIQUE,
    order_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    success BOOLEAN NOT NULL DEFAULT true,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    consumer_group VARCHAR(100),
    partition INTEGER,
    offset BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_processed_events_order_id 
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE CASCADE  -- or ON DELETE RESTRICT based on your needs
);

-- Indexes
CREATE INDEX idx_processed_events_event_id ON processed_events(event_id);
CREATE INDEX idx_processed_events_order_id ON processed_events(order_id);
CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);
CREATE INDEX idx_processed_events_event_type ON processed_events(event_type);