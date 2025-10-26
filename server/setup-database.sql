-- Server (Order Service) Database Setup
-- Run this script to create the database and user for the order service

-- Create database
CREATE DATABASE trackops_orders;

-- Create user (optional - you can use existing postgres user)
-- CREATE USER order_user WITH PASSWORD 'order_password';
-- GRANT ALL PRIVILEGES ON DATABASE trackops_orders TO order_user;

-- Connect to the order database
\c trackops_orders;

-- Create extension for UUID generation (if not exists)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- The Flyway migrations will handle table creation
-- V1__Create_orders_tables.sql
-- V2__Create_processed_events_table.sql
-- V3__Create_saga_tables.sql

-- Sample data for testing (optional)
INSERT INTO orders (customer_id, status, total_amount, street, city, state, postal_code, country, delivery_instructions) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'PENDING', 1299.99, '123 Main St', 'New York', 'NY', '10001', 'USA', 'Leave at front door'),
('550e8400-e29b-41d4-a716-446655440002', 'CONFIRMED', 199.99, '456 Oak Ave', 'Los Angeles', 'CA', '90210', 'USA', 'Ring doorbell twice'),
('550e8400-e29b-41d4-a716-446655440003', 'PROCESSING', 49.99, '789 Pine St', 'Chicago', 'IL', '60601', 'USA', 'Call before delivery');

-- Sample order items
INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, total_price) VALUES
((SELECT id FROM orders WHERE customer_id = '550e8400-e29b-41d4-a716-446655440001' LIMIT 1), 'PROD-001', 'Laptop Computer', 1, 1299.99, 1299.99),
((SELECT id FROM orders WHERE customer_id = '550e8400-e29b-41d4-a716-446655440002' LIMIT 1), 'PROD-003', 'Office Chair', 1, 199.99, 199.99),
((SELECT id FROM orders WHERE customer_id = '550e8400-e29b-41d4-a716-446655440003' LIMIT 1), 'PROD-002', 'Wireless Mouse', 1, 49.99, 49.99);

-- Verify the setup
SELECT 'Database setup completed successfully!' as status;
SELECT COUNT(*) as orders_count FROM orders;
SELECT COUNT(*) as order_items_count FROM order_items;
