#!/bin/bash
# Verify Flyway migrations after starting the server against a fresh Postgres.
# Prereqs: Server has been started at least once (e.g. ./gradlew bootRun).
# Usage: ./scripts/verify-flyway-migrations.sh [host] [port] [user] [db]
# Default: host=localhost port=5432 user=postgres db=trackops_orders

set -e
HOST="${1:-localhost}"
PORT="${2:-5432}"
USER="${3:-postgres}"
DB="${4:-trackops_orders}"
export PGPASSWORD="${PGPASSWORD:-password}"

echo "Verifying Flyway schema history and tables in ${USER}@${HOST}:${PORT}/${DB}..."
echo ""

# Check flyway_schema_history exists and list migrations
echo "=== flyway_schema_history ==="
psql -h "$HOST" -p "$PORT" -U "$USER" -d "$DB" -t -c "
  SELECT installed_rank, version, description, type, script, installed_on
  FROM flyway_schema_history
  ORDER BY installed_rank;
" || { echo "Failed. Is the server started and DB reachable?"; exit 1; }

echo ""
echo "=== Expected tables ==="
psql -h "$HOST" -p "$PORT" -U "$USER" -d "$DB" -t -c "
  SELECT tablename FROM pg_tables
  WHERE schemaname = 'public'
  ORDER BY tablename;
"

echo ""
echo "Done. If you see flyway_schema_history with V1–V6 and all application tables, Flyway is working."
