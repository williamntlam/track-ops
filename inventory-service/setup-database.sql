-- Inventory Service Database Setup
-- Run this script to create the database and user for the inventory service

-- Create database
CREATE DATABASE trackops_inventory;

-- Create user (optional - you can use existing postgres user)
-- CREATE USER inventory_user WITH PASSWORD 'inventory_password';
-- GRANT ALL PRIVILEGES ON DATABASE trackops_inventory TO inventory_user;

-- Connect to the inventory database
\c trackops_inventory;

-- Create extension for UUID generation (if not exists)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- The Flyway migrations will handle table creation
-- V1__Create_inventory_tables.sql
-- V2__Add_inventory_business_fields.sql

-- Sample data for testing (optional)
INSERT INTO inventory_items (product_id, product_name, description, available_quantity, reserved_quantity, unit_price, category, min_stock_level, max_stock_level, reorder_quantity, is_active, is_discontinued) VALUES
('PROD-001', 'Laptop Computer', 'High-performance laptop for business use', 50, 0, 1299.99, 'Electronics', 10, 100, 25, true, false),
('PROD-002', 'Wireless Mouse', 'Ergonomic wireless mouse', 200, 0, 29.99, 'Electronics', 20, 500, 50, true, false),
('PROD-003', 'Office Chair', 'Comfortable office chair with lumbar support', 30, 0, 199.99, 'Furniture', 5, 50, 15, true, false),
('PROD-004', 'Discontinued Product', 'This product is no longer available', 0, 0, 99.99, 'Electronics', 0, 0, 0, false, true),
('PROD-005', 'Low Stock Item', 'Item with low stock for testing alerts', 2, 0, 49.99, 'Accessories', 5, 20, 10, true, false);

-- Verify the setup
SELECT 'Database setup completed successfully!' as status;
SELECT COUNT(*) as inventory_items_count FROM inventory_items;
