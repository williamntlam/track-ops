-- Create inventory_items table
CREATE TABLE inventory_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id VARCHAR(255) NOT NULL UNIQUE,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    available_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    unit_price DECIMAL(10,2) NOT NULL,
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create inventory_reservations table
CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reserved_at TIMESTAMP,
    expires_at TIMESTAMP,
    released_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create indexes for performance
CREATE INDEX idx_inventory_items_product_id ON inventory_items(product_id);
CREATE INDEX idx_inventory_items_category ON inventory_items(category);
CREATE INDEX idx_inventory_items_available_quantity ON inventory_items(available_quantity);
CREATE INDEX idx_inventory_items_created_at ON inventory_items(created_at);

CREATE INDEX idx_inventory_reservations_order_id ON inventory_reservations(order_id);
CREATE INDEX idx_inventory_reservations_product_id ON inventory_reservations(product_id);
CREATE INDEX idx_inventory_reservations_status ON inventory_reservations(status);
CREATE INDEX idx_inventory_reservations_expires_at ON inventory_reservations(expires_at);
CREATE INDEX idx_inventory_reservations_created_at ON inventory_reservations(created_at);

-- Create composite indexes for common queries
CREATE INDEX idx_inventory_reservations_order_status ON inventory_reservations(order_id, status);
CREATE INDEX idx_inventory_reservations_expired ON inventory_reservations(expires_at) WHERE status = 'RESERVED';

-- Add comments for documentation
COMMENT ON TABLE inventory_items IS 'Inventory items with available and reserved quantities';
COMMENT ON TABLE inventory_reservations IS 'Inventory reservations for orders';
COMMENT ON COLUMN inventory_items.product_id IS 'Unique product identifier';
COMMENT ON COLUMN inventory_items.available_quantity IS 'Quantity available for reservation';
COMMENT ON COLUMN inventory_items.reserved_quantity IS 'Quantity currently reserved';
COMMENT ON COLUMN inventory_reservations.status IS 'Reservation status: PENDING, RESERVED, RELEASED, EXPIRED';
COMMENT ON COLUMN inventory_reservations.expires_at IS 'When the reservation expires';
