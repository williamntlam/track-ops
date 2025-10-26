-- TrackOps Complete Database Setup
-- This script sets up all databases for the TrackOps microservices system

-- ==============================================
-- 1. SERVER (ORDER SERVICE) DATABASE
-- ==============================================

-- Create order service database
CREATE DATABASE trackops_orders;

-- Connect to order database
\c trackops_orders;

-- Create extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create orders table
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10,2) NOT NULL,
    street VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    delivery_instructions TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create order_items table
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create outbox_events table
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

-- Create processed_events table
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

-- Create SAGA tables
CREATE TABLE saga_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    saga_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    current_step_index INTEGER DEFAULT 0,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    version BIGINT DEFAULT 0
);

CREATE TABLE saga_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    step_name VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    compensation_action VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    step_data TEXT,
    saga_instance_id UUID NOT NULL,
    
    CONSTRAINT fk_saga_steps_instance_id 
        FOREIGN KEY (saga_instance_id) REFERENCES saga_instances(id)
        ON DELETE CASCADE
);

-- Create indexes for order service
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_outbox_events_aggregate_id ON outbox_events(aggregate_id);
CREATE INDEX idx_outbox_events_processed ON outbox_events(processed);
CREATE INDEX idx_processed_events_event_id ON processed_events(event_id);
CREATE INDEX idx_saga_instances_aggregate_id ON saga_instances(aggregate_id);

-- ==============================================
-- 2. INVENTORY SERVICE DATABASE
-- ==============================================

-- Create inventory service database
CREATE DATABASE trackops_inventory;

-- Connect to inventory database
\c trackops_inventory;

-- Create extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

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
    min_stock_level INTEGER,
    max_stock_level INTEGER,
    reorder_quantity INTEGER,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_discontinued BOOLEAN NOT NULL DEFAULT FALSE,
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

-- Create indexes for inventory service
CREATE INDEX idx_inventory_items_product_id ON inventory_items(product_id);
CREATE INDEX idx_inventory_items_category ON inventory_items(category);
CREATE INDEX idx_inventory_items_is_active ON inventory_items(is_active);
CREATE INDEX idx_inventory_reservations_order_id ON inventory_reservations(order_id);
CREATE INDEX idx_inventory_reservations_status ON inventory_reservations(status);

-- Sample inventory data
INSERT INTO inventory_items (product_id, product_name, description, available_quantity, reserved_quantity, unit_price, category, min_stock_level, max_stock_level, reorder_quantity, is_active, is_discontinued) VALUES
('PROD-001', 'Laptop Computer', 'High-performance laptop for business use', 50, 0, 1299.99, 'Electronics', 10, 100, 25, true, false),
('PROD-002', 'Wireless Mouse', 'Ergonomic wireless mouse', 200, 0, 29.99, 'Electronics', 20, 500, 50, true, false),
('PROD-003', 'Office Chair', 'Comfortable office chair with lumbar support', 30, 0, 199.99, 'Furniture', 5, 50, 15, true, false),
('PROD-004', 'Discontinued Product', 'This product is no longer available', 0, 0, 99.99, 'Electronics', 0, 0, 0, false, true),
('PROD-005', 'Low Stock Item', 'Item with low stock for testing alerts', 2, 0, 49.99, 'Accessories', 5, 20, 10, true, false);

-- ==============================================
-- 3. EVENT RELAY SERVICE DATABASE
-- ==============================================

-- Create event relay service database
CREATE DATABASE trackops_event_relay;

-- Connect to event relay database
\c trackops_event_relay;

-- Create extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create outbox_events table (simplified version for event relay)
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

-- Create indexes for event relay service
CREATE INDEX idx_outbox_events_aggregate_id ON outbox_events(aggregate_id);
CREATE INDEX idx_outbox_events_processed ON outbox_events(processed);
CREATE INDEX idx_outbox_events_created_at ON outbox_events(created_at);

-- Sample outbox events for testing
INSERT INTO outbox_events (aggregate_id, event_type, payload, partition_key) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'ORDER_CREATED', '{"orderId":"550e8400-e29b-41d4-a716-446655440001","customerId":"550e8400-e29b-41d4-a716-446655440001","status":"PENDING","totalAmount":1299.99}', '550e8400-e29b-41d4-a716-446655440001'),
('550e8400-e29b-41d4-a716-446655440002', 'ORDER_STATUS_UPDATED', '{"orderId":"550e8400-e29b-41d4-a716-446655440002","status":"CONFIRMED","previousStatus":"PENDING"}', '550e8400-e29b-41d4-a716-446655440002');

-- ==============================================
-- VERIFICATION
-- ==============================================

-- Verify all databases and tables
\c trackops_orders;
SELECT 'Order Service Database' as service, COUNT(*) as tables FROM information_schema.tables WHERE table_schema = 'public';

\c trackops_inventory;
SELECT 'Inventory Service Database' as service, COUNT(*) as tables FROM information_schema.tables WHERE table_schema = 'public';

\c trackops_event_relay;
SELECT 'Event Relay Service Database' as service, COUNT(*) as tables FROM information_schema.tables WHERE table_schema = 'public';

-- Final success message
SELECT 'All TrackOps databases setup completed successfully!' as status;
