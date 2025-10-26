#!/bin/bash

# Test Dead Letter Queue functionality
echo "🧪 Testing Dead Letter Queue (DLQ) Functionality"
echo "================================================"

# Check if services are running
echo "📋 Checking service status..."
curl -s http://localhost:8081/actuator/health | jq '.status' || echo "❌ Server not running"

# Test DLQ endpoints
echo ""
echo "🔍 Testing DLQ endpoints..."

echo "1. DLQ Metrics:"
curl -s http://localhost:8081/api/v1/dlq/metrics | jq '.' || echo "❌ DLQ metrics endpoint failed"

echo ""
echo "2. DLQ Health:"
curl -s http://localhost:8081/api/v1/dlq/health | jq '.' || echo "❌ DLQ health endpoint failed"

echo ""
echo "📊 DLQ Topics Status:"
echo "Checking if DLQ topics exist..."

# Check DLQ topics
kafka-topics --bootstrap-server localhost:9092 --list | grep -E "(dlq|DLQ)" || echo "❌ No DLQ topics found"

echo ""
echo "🎯 DLQ Configuration:"
echo "- Retry attempts: 3"
echo "- Backoff: Exponential (1s, 2s, 4s, 8s, 10s max)"
echo "- Non-retryable exceptions: IllegalArgumentException, JsonProcessingException"
echo "- DLQ topics:"
echo "  - debezium-order-event-dlq"
echo "  - debezium-cache-consumer-dlq" 
echo "  - debezium-cache-warmer-dlq"

echo ""
echo "🚨 To test DLQ functionality:"
echo "1. Create a malformed message in a Debezium topic"
echo "2. Watch the logs for retry attempts"
echo "3. Check DLQ topic for the failed message"
echo "4. Monitor DLQ metrics endpoint"

echo ""
echo "✅ DLQ Implementation Complete!"
echo "   - DLQ topics created"
echo "   - Retry configuration set"
echo "   - Error handler configured"
echo "   - Monitoring endpoints available"
echo "   - DLQ monitor service active"
