-- Outbox for inventory reserve requests: enqueue in same transaction as order processing,
-- then a background processor sends to Kafka with retries and marks SENT or FAILED.
CREATE TABLE inventory_reserve_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ,
    last_error TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_inventory_reserve_outbox_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_inventory_reserve_outbox_status ON inventory_reserve_outbox(status);
CREATE INDEX idx_inventory_reserve_outbox_pending ON inventory_reserve_outbox(created_at)
    WHERE status = 'PENDING';

COMMENT ON TABLE inventory_reserve_outbox IS 'Outbox for inventory reserve requests; processor sends to Kafka with retries';
