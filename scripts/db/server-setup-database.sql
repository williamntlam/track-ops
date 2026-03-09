-- Server (Order Service) Database Setup
-- Run this once to create the database and UUID extension.
-- All table creation is done by Flyway on first application startup.

-- Create database
CREATE DATABASE trackops_orders;

-- Connect to the order database (run manually: \c trackops_orders)
-- Then create extension for UUID generation:
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- After running this script, start the application (e.g. ./gradlew bootRun).
-- Flyway will create flyway_schema_history and run migrations from
-- src/main/resources/db/migration/ (V1__... through V6__...).
-- Verify: SELECT * FROM flyway_schema_history;
