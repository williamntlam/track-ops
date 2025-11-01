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

# Start infrastructure services
echo "📦 Starting databases and message brokers..."
docker compose up -d postgres-server postgres-inventory postgres-event-relay redis kafka zookeeper debezium-connect

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 30

# Check service health
echo "🔍 Checking service health..."

# Check PostgreSQL services
echo "Checking PostgreSQL services..."
docker compose exec postgres-server pg_isready -U postgres -d trackops_orders
docker compose exec postgres-inventory pg_isready -U postgres -d trackops_inventory
docker compose exec postgres-event-relay pg_isready -U postgres -d trackops_event_relay

# Check Redis
echo "Checking Redis..."
docker compose exec redis redis-cli ping

# Check Kafka
echo "Checking Kafka..."
docker compose exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# Check Debezium Connect
echo "Checking Debezium Connect..."
sleep 10  # Give Debezium Connect time to start
for i in {1..10}; do
    if curl -s http://localhost:8083/connectors > /dev/null 2>&1; then
        echo "✅ Debezium Connect is ready!"
        break
    fi
    echo "⏳ Attempt $i/10: Debezium Connect not ready yet..."
    sleep 3
done

# Initialize databases
echo "🗄️ Initializing databases..."
./scripts/init-databases.sh

echo "✅ Infrastructure services are ready!"
echo ""
echo "🌐 Service URLs:"
echo "  - Server (Order Service): http://localhost:8081"
echo "  - Inventory Service: http://localhost:8082"
echo "  - Event Relay Service: http://localhost:8083"
echo "  - Debezium Connect: http://localhost:8083"
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
echo "🎯 To start the microservices, run:"
echo "  ./gradlew bootRun --args='--spring.profiles.active=docker'"
