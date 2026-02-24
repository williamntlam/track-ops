#!/bin/bash

# MirrorMaker 2 Connector Setup Script
# Registers MM2 Source, Checkpoint, and Heartbeat connectors with the MM2 Connect worker.
# Prerequisites: source Kafka (docker/kafka.yml), target Kafka (docker/kafka-target.yml),
#                MM2 Connect (docker/mirror-maker-2.yml) running; MM2 REST API on port 8086.

set -e
cd "$(dirname "$0")/.."

MM2_URL="${MM2_CONNECT_URL:-http://localhost:8086}"
CONNECTORS_DIR="docker/mm2"

echo "🔧 Setting up MirrorMaker 2 connectors (MM2 URL: $MM2_URL)..."

# Wait for MM2 Connect to be ready
echo "⏳ Waiting for MM2 Connect to be ready..."
max_attempts=30
attempt=1
while [ $attempt -le $max_attempts ]; do
  if curl -sf "$MM2_URL/connectors" > /dev/null 2>&1; then
    echo "✅ MM2 Connect is ready!"
    break
  fi
  echo "⏳ Attempt $attempt/$max_attempts: MM2 Connect not ready yet..."
  sleep 5
  ((attempt++))
done

if [ $attempt -gt $max_attempts ]; then
  echo "❌ MM2 Connect failed to respond after $max_attempts attempts. Is the container running?"
  exit 1
fi

existing_connectors=$(curl -s "$MM2_URL/connectors" || echo "[]")

register_connector() {
  local name=$1
  local file=$2
  if echo "$existing_connectors" | grep -q "\"$name\""; then
    echo "✅ Connector $name already exists (restarting to pick up config changes)..."
    curl -s -X POST "$MM2_URL/connectors/$name/restart" > /dev/null || true
  else
    echo "📦 Creating connector $name..."
    response=$(curl -s -w "\n%{http_code}" -X POST \
      -H "Content-Type: application/json" \
      -d @"$CONNECTORS_DIR/$file" \
      "$MM2_URL/connectors")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    if [ "$http_code" != "201" ] && [ "$http_code" != "200" ]; then
      echo "❌ Failed to create $name (HTTP $http_code): $body"
      return 1
    fi
    if echo "$body" | grep -q '"error_code"'; then
      echo "❌ Failed to create $name: $body"
      return 1
    fi
    echo "✅ Connector $name created."
  fi
}

register_connector "trackops-mm2-source"       "mm2-source-connector.json"    || exit 1
register_connector "trackops-mm2-checkpoint"   "mm2-checkpoint-connector.json" || exit 1
register_connector "trackops-mm2-heartbeat"    "mm2-heartbeat-connector.json"  || exit 1

echo ""
echo "🎉 MirrorMaker 2 connectors configured!"
echo ""
echo "📊 MM2 Connect REST: $MM2_URL"
echo "   List connectors:  curl -s $MM2_URL/connectors"
echo "   Source status:    curl -s $MM2_URL/connectors/trackops-mm2-source/status"
echo "   Checkpoint:       curl -s $MM2_URL/connectors/trackops-mm2-checkpoint/status"
echo "   Heartbeat:        curl -s $MM2_URL/connectors/trackops-mm2-heartbeat/status"
echo ""
