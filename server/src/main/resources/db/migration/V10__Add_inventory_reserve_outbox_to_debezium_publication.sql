-- Add inventory_reserve_outbox to the Debezium publication so the outbox CDC connector can stream it.
-- Requires that the publication already exists (e.g. created by infra/setup).
-- If run by a user without ALTER permission on the publication, this will fail; add the table manually:
--   ALTER PUBLICATION debezium_publication ADD TABLE inventory_reserve_outbox;
ALTER PUBLICATION debezium_publication ADD TABLE inventory_reserve_outbox;
