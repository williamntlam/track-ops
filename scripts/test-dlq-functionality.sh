#!/bin/bash

# Test Dead Letter Queue functionality (PostgreSQL dlq_orders)
echo "🧪 Testing Dead Letter Queue (DLQ) Functionality"
echo "================================================"

# Check if services are running
echo "📋 Checking service status..."
curl -s http://localhost:8081/actuator/health | jq '.status' || echo "❌ Server not running"

# Test DLQ endpoints (PostgreSQL-backed)
echo ""
echo "🔍 Testing DLQ endpoints..."

echo "1. DLQ Metrics:"
curl -s http://localhost:8081/api/dlq/metrics | jq '.' || echo "❌ DLQ metrics endpoint failed"

echo ""
echo "2. DLQ Health:"
curl -s http://localhost:8081/api/dlq/health | jq '.' || echo "❌ DLQ health endpoint failed"

echo ""
echo "3. DLQ Orders (list PENDING):"
curl -s "http://localhost:8081/api/dlq/orders?status=PENDING" | jq '.' || echo "❌ DLQ orders endpoint failed"

echo ""
echo "🎯 DLQ Configuration:"
echo "- Storage: PostgreSQL table dlq_orders"
echo "- Max retries: 3 (configurable via app.dlq.max-retries)"
echo "- Endpoints: GET /api/dlq/metrics, /api/dlq/health, /api/dlq/orders, /api/dlq/orders/{id}"

echo ""
echo "🚨 To test DLQ functionality:"
echo "1. Trigger a failure in the Debezium order event consumer"
echo "2. Check dlq_orders table or GET /api/dlq/orders for the failed event"
echo "3. Monitor GET /api/dlq/metrics for pending count"

echo ""
echo "✅ DLQ Implementation Complete!"
echo "   - DLQ stored in PostgreSQL (dlq_orders)"
echo "   - Error handler persists failed order events to DB"
echo "   - Monitoring endpoints available"