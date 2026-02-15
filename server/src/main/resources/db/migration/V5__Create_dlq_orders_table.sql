-- Dead Letter Queue for failed order events (PostgreSQL DLQ)
CREATE TABLE dlq_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    message_type VARCHAR(100),
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_log TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_retry_at TIMESTAMP,
    next_retry_at TIMESTAMP
);

-- Index for polling pending items with exponential backoff
CREATE INDEX idx_dlq_pending ON dlq_orders (status, next_retry_at)
WHERE status = 'PENDING';

CREATE INDEX idx_dlq_orders_order_id ON dlq_orders (order_id);
CREATE INDEX idx_dlq_orders_status ON dlq_orders (status);
CREATE INDEX idx_dlq_orders_created_at ON dlq_orders (created_at);

COMMENT ON TABLE dlq_orders IS 'Failed order events for retry and analysis';
COMMENT ON COLUMN dlq_orders.status IS 'PENDING, PROCESSING, COMPLETED, PERMANENT_FAILURE';
