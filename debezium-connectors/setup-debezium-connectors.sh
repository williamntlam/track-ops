#!/bin/bash

# Debezium Connector Setup Script
# This script sets up the Debezium connectors for TrackOps

echo "🔧 Setting up Debezium connectors for TrackOps..."

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

# Create the orders connector
echo "📦 Creating TrackOps Orders connector..."
response=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d @debezium-connectors/trackops-orders-connector.json \
  http://localhost:8083/connectors)

if echo "$response" | grep -q "error"; then
    echo "❌ Failed to create connector: $response"
    exit 1
else
    echo "✅ TrackOps Orders connector created successfully!"
fi

# Verify connector status
echo "🔍 Verifying connector status..."
curl -s http://localhost:8083/connectors/trackops-orders-connector/status | jq '.'

echo ""
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
