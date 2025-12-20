#!/bin/bash

# TrackOps Infrastructure Startup Script
# Change to project root directory
cd "$(dirname "$0")/.."

echo "🚀 Starting TrackOps Infrastructure..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Create network if it doesn't exist
echo "🌐 Ensuring Docker network exists..."
docker network create trackops-network 2>/dev/null || echo "Network already exists"

# Start infrastructure services
echo "📦 Starting databases and message brokers..."

# Start PostgreSQL services
echo "Starting PostgreSQL services..."
docker compose -f docker/postgres.yml up -d

# Start Redis
echo "Starting Redis..."
docker compose -f docker/redis.yml up -d

# Start Kafka and Schema Registry
echo "Starting Kafka and Schema Registry..."
docker compose -f docker/kafka.yml up -d

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 30

# Check service health
echo "🔍 Checking service health..."

# Check PostgreSQL services
echo "Checking PostgreSQL services..."
docker exec trackops-postgres-server pg_isready -U postgres -d trackops_orders || echo "⚠️  postgres-server not ready"
docker exec trackops-postgres-inventory pg_isready -U postgres -d trackops_inventory || echo "⚠️  postgres-inventory not ready"
docker exec trackops-postgres-event-relay pg_isready -U postgres -d trackops_event_relay || echo "⚠️  postgres-event-relay not ready"

# Check Redis
echo "Checking Redis..."
docker exec trackops-redis redis-cli ping || echo "⚠️  redis not ready"

# Check Kafka
echo "Checking Kafka..."
docker exec trackops-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 || echo "⚠️  kafka not ready"

# Check Schema Registry
echo "Checking Schema Registry..."
sleep 5  # Give Schema Registry time to start
for i in {1..10}; do
    if curl -s http://localhost:8085/subjects > /dev/null 2>&1; then
        echo "✅ Schema Registry is ready!"
        break
    fi
    echo "⏳ Attempt $i/10: Schema Registry not ready yet..."
    sleep 3
done

# Note: Debezium Connect will be started by start-all-microservices.sh after Kafka is healthy
echo "ℹ️  Debezium Connect will be started by the main startup script after Kafka is confirmed healthy"

# Initialize databases
echo "🗄️ Initializing databases..."
./scripts/init-databases.sh

echo "✅ Infrastructure services are ready!"
echo ""
echo "⚠️  NOTE: Infrastructure services are running, but microservices are NOT started yet."
echo ""
echo "🌐 Infrastructure Service URLs:"
echo "  - Debezium Connect: http://localhost:8083"
echo "  - Schema Registry: http://localhost:8085"
echo "  - Kafka UI: http://localhost:8080"
echo "  - pgAdmin: http://localhost:5050 (admin@trackops.com / admin)"
echo ""
echo "📊 Database Connections:"
echo "  - Server DB: localhost:5432/trackops_orders"
echo "  - Inventory DB: localhost:5433/trackops_inventory"
echo "  - Event Relay DB: localhost:5434/trackops_event_relay"
echo "  - Redis: localhost:6379"
echo "  - Kafka: localhost:9092"
echo ""
echo "🎯 To start the microservices (Order, Inventory, Event Relay), run:"
echo "  ./scripts/start-all-microservices.sh"
echo ""
echo "   Or start them individually:"
echo "   - Order Service:     cd server && ./gradlew bootRun"
echo "                        OR: docker compose -f docker/trackops-server.yml up -d"
echo "   - Inventory Service: cd inventory-service && ./gradlew bootRun"
echo "                        OR: docker compose -f docker/inventory-service.yml up -d"
echo "   - Event Relay:       cd event-relay-service && ./gradlew bootRun"
echo "                        OR: docker compose -f docker/event-relay-service.yml up -d"
echo ""
echo "🌐 Microservice URLs (after starting):"
echo "  - Order Service:     http://localhost:8081"
echo "  - Inventory Service: http://localhost:8082"
echo "  - Event Relay Service: http://localhost:8084"
