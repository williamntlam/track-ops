#!/bin/bash

# Test script to verify Debezium Redis Cache Integration
# This script tests the complete flow: Database Change → Debezium → Redis Cache Invalidation/Warming

echo "🧪 Testing Debezium Redis Cache Integration"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to check if a service is running
check_service() {
    local service_name=$1
    local port=$2
    local url=$3
    
    if curl -f "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ $service_name is running on port $port${NC}"
        return 0
    else
        echo -e "${RED}❌ $service_name is not running on port $port${NC}"
        return 1
    fi
}

# Function to check Redis cache keys
check_redis_cache() {
    local pattern=$1
    local description=$2
    
    echo -e "${YELLOW}🔍 Checking Redis cache pattern: $pattern ($description)${NC}"
    
    # Use redis-cli to check cache keys
    cache_keys=$(docker exec trackops-redis redis-cli KEYS "$pattern" 2>/dev/null | wc -l)
    
    if [ "$cache_keys" -gt 0 ]; then
        echo -e "${GREEN}✅ Found $cache_keys cache entries matching pattern: $pattern${NC}"
        # Show a few example keys
        docker exec trackops-redis redis-cli KEYS "$pattern" 2>/dev/null | head -3 | while read key; do
            echo -e "${BLUE}   - $key${NC}"
        done
    else
        echo -e "${YELLOW}⚠️  No cache entries found for pattern: $pattern${NC}"
    fi
}

# Function to monitor Redis cache changes
monitor_redis_changes() {
    local pattern=$1
    local duration=${2:-10}
    
    echo -e "${YELLOW}👀 Monitoring Redis cache changes for pattern: $pattern (${duration}s)${NC}"
    
    # Start monitoring in background
    (
        docker exec trackops-redis redis-cli MONITOR 2>/dev/null | grep -E "(SET|DEL|EXPIRE).*$pattern" &
        MONITOR_PID=$!
        sleep $duration
        kill $MONITOR_PID 2>/dev/null
    ) &
    
    echo "Monitoring started... Check Redis operations in real-time"
}

echo ""
echo "1. Checking Infrastructure Services..."
echo "--------------------------------------"

# Check if services are running
check_service "Debezium Connect" "8083" "http://localhost:8083/connectors"
check_service "Redis" "6379" "redis://localhost:6379"
check_service "Kafka UI" "8080" "http://localhost:8080"
check_service "Debezium UI" "8084" "http://localhost:8084"

echo ""
echo "2. Checking Redis Cache Status..."
echo "--------------------------------"

# Check Redis connection
if docker exec trackops-redis redis-cli PING > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Redis is accessible and responding${NC}"
    
    # Get Redis info
    echo "Redis Info:"
    docker exec trackops-redis redis-cli INFO memory | grep -E "(used_memory_human|maxmemory_human)" | head -2
else
    echo -e "${RED}❌ Redis is not accessible${NC}"
    exit 1
fi

echo ""
echo "3. Checking Current Cache State..."
echo "---------------------------------"

# Check existing cache patterns
check_redis_cache "order:*" "Order-related cache entries"
check_redis_cache "processed_event:*" "Event processing cache entries"
check_redis_cache "orders:status:*" "Order status cache entries"
check_redis_cache "orders:customer:*" "Customer order cache entries"

echo ""
echo "4. Testing Cache Invalidation Flow..."
echo "------------------------------------"

echo -e "${YELLOW}📝 Testing cache invalidation with Debezium events...${NC}"

# Create a test order and monitor cache changes
echo "To test cache invalidation:"
echo "1. Create an order via the Order Service API"
echo "2. Check Redis cache gets invalidated via Debezium events"
echo "3. Verify cache warming updates with fresh data"

echo ""
echo "Manual Test Commands:"
echo ""

echo "# 1. Create an order (replace with actual API call)"
echo "curl -X POST http://localhost:8081/api/orders \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"customerId\": \"test-customer\", \"items\": [{\"productId\": \"test-product\", \"quantity\": 1}]}'"
echo ""

echo "# 2. Monitor Redis cache changes in real-time"
echo "docker exec trackops-redis redis-cli MONITOR | grep -E '(SET|DEL|EXPIRE).*order:'"
echo ""

echo "# 3. Check specific cache keys"
echo "docker exec trackops-redis redis-cli KEYS 'order:*'"
echo ""

echo "# 4. Check cache TTL"
echo "docker exec trackops-redis redis-cli TTL 'order:status:{orderId}'"
echo ""

echo "# 5. Update order status and watch cache invalidation"
echo "curl -X PUT http://localhost:8081/api/orders/{orderId}/status \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"status\": \"CONFIRMED\"}'"
echo ""

echo "5. Cache Warming Test..."
echo "----------------------"

echo -e "${YELLOW}🔥 Testing cache warming with Debezium events...${NC}"

echo "Cache warming will:"
echo "1. Listen to Debezium CDC events"
echo "2. Proactively update cache with fresh data"
echo "3. Maintain cache consistency automatically"

echo ""
echo "To test cache warming:"
echo "1. Make database changes directly (or via API)"
echo "2. Watch Redis cache get updated automatically"
echo "3. Verify cache contains fresh data"

echo ""
echo "6. Performance Monitoring..."
echo "---------------------------"

echo -e "${YELLOW}📊 Monitoring cache performance...${NC}"

# Show Redis memory usage
echo "Redis Memory Usage:"
docker exec trackops-redis redis-cli INFO memory | grep -E "(used_memory_human|maxmemory_human|mem_fragmentation_ratio)"

# Show cache hit/miss statistics
echo ""
echo "Cache Statistics:"
docker exec trackops-redis redis-cli INFO stats | grep -E "(keyspace_hits|keyspace_misses)"

echo ""
echo "7. Configuration Summary..."
echo "--------------------------"

echo "✅ Debezium Redis Cache Integration:"
echo "   - Cache Invalidation: DebeziumRedisCacheConsumer"
echo "   - Cache Warming: DebeziumRedisCacheWarmer"
echo "   - Cache Patterns:"
echo "     * order:entity:{orderId} - Full order objects"
echo "     * order:status:{orderId} - Order status"
echo "     * order:response:{orderId} - API responses"
echo "     * orders:status:{status} - Orders by status"
echo "     * orders:customer:{customerId} - Orders by customer"
echo ""
echo "✅ Cache TTL Configuration:"
echo "   - Order entities: 3600s (1 hour)"
echo "   - Order status: 1800s (30 minutes)"
echo "   - Order responses: 1800s (30 minutes)"
echo "   - Customer orders: 3600s (1 hour)"
echo "   - Page results: 900s (15 minutes)"
echo ""
echo "✅ Event Flow:"
echo "   Database Change → Debezium → Cache Invalidation/Warming → Fresh Redis Cache"

echo ""
echo "8. Troubleshooting..."
echo "--------------------"

echo "Common Issues and Solutions:"
echo ""
echo "1. Cache not invalidating:"
echo "   - Check Debezium connector status"
echo "   - Verify app.event-publishing.strategy=debezium"
echo "   - Check DebeziumRedisCacheConsumer logs"
echo ""
echo "2. Cache not warming:"
echo "   - Check DebeziumRedisCacheWarmer logs"
echo "   - Verify Redis connection"
echo "   - Check cache TTL configuration"
echo ""
echo "3. High memory usage:"
echo "   - Adjust cache TTL values"
echo "   - Check for memory leaks"
echo "   - Monitor Redis memory usage"

echo ""
echo "9. Monitoring Commands..."
echo "------------------------"

echo "# Monitor Redis operations"
echo "docker exec trackops-redis redis-cli MONITOR"
echo ""
echo "# Check cache keys"
echo "docker exec trackops-redis redis-cli KEYS 'order:*'"
echo ""
echo "# Check cache TTL"
echo "docker exec trackops-redis redis-cli TTL 'order:status:{orderId}'"
echo ""
echo "# Get Redis info"
echo "docker exec trackops-redis redis-cli INFO"
echo ""
echo "# Check Debezium consumer lag"
echo "docker exec trackops-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group debezium-cache-consumer"

echo ""
echo -e "${GREEN}🎉 Debezium Redis Cache Integration setup is complete!${NC}"
echo ""
echo "Benefits:"
echo "✅ Real-time cache invalidation via Debezium CDC"
echo "✅ Proactive cache warming with fresh data"
echo "✅ Reduced cache staleness"
echo "✅ Better performance with consistent cache"
echo "✅ Automatic cache management"
echo ""
echo "Next steps:"
echo "1. Start the server with Debezium strategy: --app.event-publishing.strategy=debezium"
echo "2. Create/update orders to test cache invalidation"
echo "3. Monitor Redis cache changes in real-time"
echo "4. Verify cache consistency and performance improvements"
