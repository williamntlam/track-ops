-- Event Relay Service Database Setup
-- Run this script to create the database and user for the event relay service

-- Create database
CREATE DATABASE trackops_event_relay;

-- Create user (optional - you can use existing postgres user)
-- CREATE USER event_relay_user WITH PASSWORD 'event_relay_password';
-- GRANT ALL PRIVILEGES ON DATABASE trackops_event_relay TO event_relay_user;

-- Connect to the event relay database
\c trackops_event_relay;

-- Create extension for UUID generation (if not exists)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- The Flyway migrations will handle table creation
-- V1__Create_outbox_events_table.sql

-- Sample outbox events for testing (optional)
INSERT INTO outbox_events (aggregate_id, event_type, payload, partition_key) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'ORDER_CREATED', '{"orderId":"550e8400-e29b-41d4-a716-446655440001","customerId":"550e8400-e29b-41d4-a716-446655440001","status":"PENDING","totalAmount":1299.99}', '550e8400-e29b-41d4-a716-446655440001'),
('550e8400-e29b-41d4-a716-446655440002', 'ORDER_STATUS_UPDATED', '{"orderId":"550e8400-e29b-41d4-a716-446655440002","status":"CONFIRMED","previousStatus":"PENDING"}', '550e8400-e29b-41d4-a716-446655440002'),
('550e8400-e29b-41d4-a716-446655440003', 'ORDER_CANCELLED', '{"orderId":"550e8400-e29b-41d4-a716-446655440003","reason":"Customer requested cancellation"}', '550e8400-e29b-41d4-a716-446655440003');

-- Verify the setup
SELECT 'Database setup completed successfully!' as status;
SELECT COUNT(*) as outbox_events_count FROM outbox_events;
SELECT COUNT(*) as unprocessed_events FROM outbox_events WHERE processed = FALSE;
