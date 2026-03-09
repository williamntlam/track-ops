#!/bin/bash

# Debezium Connector Setup Script
# This script sets up the Debezium connectors for TrackOps

set -e

# Ensure we run from the project root so relative paths work
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$PROJECT_ROOT"

echo "🔧 Setting up Debezium connectors for TrackOps (project root: $PROJECT_ROOT)..."

# Wait for Debezium Connect to be ready
echo "⏳ Waiting for Debezium Connect to be ready..."
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    if curl -f http://localhost:8083/connectors > /dev/null 2>&1; then
        echo "✅ Debezium Connect is ready!"
        break
    fi
    echo "⏳ Attempt $attempt/$max_attempts: Debezium Connect not ready yet..."
    sleep 5
    ((attempt++))
done

if [ $attempt -gt $max_attempts ]; then
    echo "❌ Debezium Connect failed to start after $max_attempts attempts"
    exit 1
fi

# Check if connector already exists
echo "🔍 Checking if TrackOps Orders connector already exists..."
existing_connectors=$(curl -s http://localhost:8083/connectors)

if echo "$existing_connectors" | grep -q "trackops-orders-connector"; then
    echo "✅ TrackOps Orders connector already exists!"
    echo "🔄 Restarting connector to apply any configuration changes..."
    response=$(curl -s -X POST \
      http://localhost:8083/connectors/trackops-orders-connector/restart)
else
    echo "📦 Creating TrackOps Orders connector..."
    response=$(curl -s -X POST \
      -H "Content-Type: application/json" \
      -d @"$PROJECT_ROOT/services/debezium-connectors/trackops-orders-connector.json" \
      http://localhost:8083/connectors)
fi

if echo "$response" | grep -q "error"; then
    echo "❌ Failed to create/update connector: $response"
    exit 1
else
    echo "✅ TrackOps Orders connector configured successfully!"
fi

# Verify connector status (with retry to avoid race right after creation)
echo "🔍 Verifying connector status..."
status_max_attempts=30
status_attempt=1
while [ $status_attempt -le $status_max_attempts ]; do
    if curl -sf http://localhost:8083/connectors/trackops-orders-connector/status > /dev/null 2>&1; then
        echo "✅ Connector status endpoint is available!"
        break
    fi
    echo "⏳ Attempt $status_attempt/$status_max_attempts: status not available yet..."
    sleep 2
    ((status_attempt++))
done

if [ $status_attempt -gt $status_max_attempts ]; then
    echo "⚠️  Connector status endpoint not available after $status_max_attempts attempts. Proceeding anyway."
else
    curl -s http://localhost:8083/connectors/trackops-orders-connector/status
    echo ""
fi

# Inventory reserve outbox connector (for app.inventory.reserve-request.mode=cdc)
echo "🔍 Checking if TrackOps Inventory Reserve Outbox connector exists..."
if echo "$existing_connectors" | grep -q "trackops-inventory-reserve-outbox-connector"; then
    echo "✅ TrackOps Inventory Reserve Outbox connector already exists!"
    echo "🔄 Restarting connector..."
    curl -s -X POST http://localhost:8083/connectors/trackops-inventory-reserve-outbox-connector/restart > /dev/null
else
    echo "📦 Creating TrackOps Inventory Reserve Outbox connector..."
    outbox_response=$(curl -s -X POST \
      -H "Content-Type: application/json" \
      -d @"$PROJECT_ROOT/services/debezium-connectors/trackops-inventory-reserve-outbox-connector.json" \
      http://localhost:8083/connectors)
    if echo "$outbox_response" | grep -q "error"; then
        echo "⚠️  Failed to create inventory outbox connector (ensure publication includes inventory_reserve_outbox): $outbox_response"
    else
        echo "✅ TrackOps Inventory Reserve Outbox connector configured!"
    fi
fi

echo "🎉 Debezium setup completed!"
echo ""
echo "📊 Management URLs:"
echo "  - Debezium Connect: http://localhost:8083"
echo "  - Debezium UI: http://localhost:8084"
echo "  - Kafka UI: http://localhost:8080"
echo ""
echo "🔍 To check connector status:"
echo "  curl http://localhost:8083/connectors/trackops-orders-connector/status"
echo ""
echo "🛑 To delete connector:"
echo "  curl -X DELETE http://localhost:8083/connectors/trackops-orders-connector"
