-- Index for cache pre-warm query: most recently updated orders first
CREATE INDEX IF NOT EXISTS idx_orders_updated_at ON orders(updated_at DESC);
