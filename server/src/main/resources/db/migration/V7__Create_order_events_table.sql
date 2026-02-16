-- Append-only Event Store for order event sourcing and auditability
CREATE TABLE order_events (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL,
    event_type TEXT NOT NULL,
    payload JSONB NOT NULL,
    schema_version INT NOT NULL,
    sequence_number INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_order_events_order_sequence UNIQUE (order_id, sequence_number),
    CONSTRAINT fk_order_events_order_id
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE CASCADE
);

-- Index for fast retrieval of an order's event history (ordered by sequence)
CREATE INDEX idx_order_events_aggregate ON order_events (order_id, sequence_number);

-- GIN index for searching inside the JSON payloads
CREATE INDEX idx_order_events_payload_search ON order_events USING GIN (payload);

-- Optional: index for time-based queries and audit trails
CREATE INDEX idx_order_events_created_at ON order_events (created_at);

-- Documentation
COMMENT ON TABLE order_events IS 'Append-only event store for order event sourcing and auditability';
COMMENT ON COLUMN order_events.order_id IS 'Order aggregate ID';
COMMENT ON COLUMN order_events.event_type IS 'Event type (e.g. ORDER_PLACED, PaymentProcessed, OrderShipped)';
COMMENT ON COLUMN order_events.payload IS 'Event data as JSONB for flexible schema and querying';
COMMENT ON COLUMN order_events.schema_version IS 'Version of the payload structure for evolution';
COMMENT ON COLUMN order_events.sequence_number IS 'Strict ordering of events per order (1, 2, 3, ...)';
