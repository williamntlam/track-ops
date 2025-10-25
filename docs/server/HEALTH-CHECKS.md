# TrackOps Health Checks

This document describes the comprehensive health check system implemented in TrackOps server for monitoring and operational readiness.

## 🏥 Health Check Overview

TrackOps implements a multi-layered health check system that monitors:
- **Database** - PostgreSQL connectivity and performance
- **Redis** - Cache connectivity and operations
- **Kafka** - Message broker connectivity and publishing
- **Application** - JVM health, memory usage, and uptime

## 📊 Health Check Endpoints

### Spring Boot Actuator Endpoints

#### Main Health Endpoint
```bash
GET /actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "application": {
      "status": "UP",
      "details": {
        "application": "trackops-server",
        "profile": "default",
        "uptime": "2h 15m 30s",
        "heapUsagePercent": "45.2%",
        "availableProcessors": 4
      }
    },
    "database": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "version": "PostgreSQL 15.3",
        "queryTime": "15ms",
        "totalOrders": 1250,
        "totalEvents": 3400
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "redis": "Redis",
        "version": "7.0.5",
        "operationTime": "5ms",
        "usedMemory": "12.5MB",
        "cacheKeyCount": 450
      }
    },
    "kafka": {
      "status": "UP",
      "details": {
        "kafka": "Apache Kafka",
        "bootstrapServers": "kafka:9092",
        "publishTime": "25ms",
        "topics": "ORDER_CREATED, ORDER_STATUS_UPDATED, ORDER_CANCELLED, ORDER_DELIVERED"
      }
    }
  }
}
```

#### Individual Component Health
```bash
# Database health
GET /actuator/health/database

# Redis health
GET /actuator/health/redis

# Kafka health
GET /actuator/health/kafka

# Application health
GET /actuator/health/application
```

### Custom Health Endpoints

#### Simple Health Check
```bash
GET /health/simple
```

**Response:**
```json
{
  "status": "UP",
  "message": "All services are healthy"
}
```

#### Detailed Health Check
```bash
GET /health/detailed
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "database": { ... },
    "redis": { ... },
    "kafka": { ... },
    "application": { ... }
  },
  "timestamp": 1642248000000
}
```

#### Kubernetes Readiness Probe
```bash
GET /health/ready
```

**Response:**
```json
{
  "status": "READY",
  "message": "Application is ready to receive traffic"
}
```

#### Kubernetes Liveness Probe
```bash
GET /health/live
```

**Response:**
```json
{
  "status": "ALIVE",
  "message": "Application is alive",
  "timestamp": "1642248000000"
}
```

## 🔍 Health Check Details

### Database Health Indicator

**Checks:**
- ✅ PostgreSQL connectivity
- ✅ Query performance (< 5 seconds)
- ✅ Database statistics (order count, event count)
- ✅ Connection pool status

**Metrics:**
- Query execution time
- Total orders in database
- Total events in outbox
- Total SAGA instances

**Status Conditions:**
- **UP**: Query time < 5 seconds, connection successful
- **DOWN**: Query timeout > 5 seconds or connection failed

### Redis Health Indicator

**Checks:**
- ✅ Redis connectivity (PING)
- ✅ Read/write operations
- ✅ Memory usage
- ✅ Cache key count

**Metrics:**
- Operation execution time
- Memory usage (used/total)
- Connected clients
- Cache key count
- Server uptime

**Status Conditions:**
- **UP**: Operations < 1 second, PING successful
- **DOWN**: Operation timeout > 1 second or connection failed

### Kafka Health Indicator

**Checks:**
- ✅ Kafka broker connectivity
- ✅ Message publishing capability
- ✅ Topic availability
- ✅ Consumer group status

**Metrics:**
- Message publish time
- Bootstrap servers
- Available topics
- Consumer group information

**Status Conditions:**
- **UP**: Publish time < 5 seconds, broker accessible
- **DOWN**: Publish timeout > 5 seconds or broker unreachable

### Application Health Indicator

**Checks:**
- ✅ JVM health and memory usage
- ✅ Application uptime
- ✅ System resources
- ✅ Java version and runtime info

**Metrics:**
- Heap memory usage percentage
- Non-heap memory usage
- Available processors
- Application uptime
- JVM version and vendor

**Status Conditions:**
- **UP**: Memory usage < 95%, application responding
- **DOWN**: Memory usage > 95% or application unresponsive

## ⚠️ Health Check Warnings

### Performance Warnings
- **Database**: Query time > 1 second
- **Redis**: Operation time > 100ms
- **Kafka**: Publish time > 500ms
- **Application**: Memory usage > 80%

### Critical Thresholds
- **Database**: Query timeout > 5 seconds
- **Redis**: Operation timeout > 1 second
- **Kafka**: Publish timeout > 5 seconds
- **Application**: Memory usage > 95%

## 🚀 Usage Examples

### Load Balancer Health Check
```bash
# Simple health check for load balancers
curl -f http://localhost:8080/health/simple

# Detailed health check for monitoring
curl http://localhost:8080/actuator/health
```

### Kubernetes Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: trackops-server
spec:
  template:
    spec:
      containers:
      - name: trackops-server
        image: trackops-server:latest
        ports:
        - containerPort: 8080
        livenessProbe:
          httpGet:
            path: /health/live
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

### Monitoring Integration
```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Application info
curl http://localhost:8080/actuator/info
```

## 🔧 Configuration

### Application Properties
```properties
# Health check configuration
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.endpoint.health.show-components=always
management.health.defaults.enabled=true
management.health.db.enabled=true
management.health.redis.enabled=true
management.health.diskspace.enabled=true
management.health.diskspace.threshold=100MB
```

### Custom Health Check Configuration
```yaml
trackops:
  health:
    database:
      enabled: true
      timeout: 5s
    redis:
      enabled: true
      timeout: 3s
    kafka:
      enabled: true
      timeout: 5s
    custom:
      enabled: true
```

## 📈 Monitoring Integration

### Prometheus Metrics
Health check results are exposed as Prometheus metrics:
```
# Health check status (1 = UP, 0 = DOWN)
health_status{component="database"} 1
health_status{component="redis"} 1
health_status{component="kafka"} 1
health_status{component="application"} 1

# Health check duration
health_check_duration_seconds{component="database"} 0.015
health_check_duration_seconds{component="redis"} 0.005
health_check_duration_seconds{component="kafka"} 0.025
```

### Grafana Dashboard
Create dashboards to visualize:
- Health check status over time
- Component response times
- Error rates and patterns
- Resource usage trends

### Alerting Rules
Set up alerts for:
- Any component health check failure
- Performance degradation warnings
- Critical resource usage thresholds
- Service unavailability

## 🛠️ Troubleshooting

### Common Issues

#### Database Health Check Failing
```bash
# Check database connectivity
curl http://localhost:8080/actuator/health/database

# Common causes:
# - Database server down
# - Network connectivity issues
# - Authentication problems
# - Query timeout
```

#### Redis Health Check Failing
```bash
# Check Redis connectivity
curl http://localhost:8080/actuator/health/redis

# Common causes:
# - Redis server down
# - Memory exhaustion
# - Network connectivity issues
# - Authentication problems
```

#### Kafka Health Check Failing
```bash
# Check Kafka connectivity
curl http://localhost:8080/actuator/health/kafka

# Common causes:
# - Kafka broker down
# - Network connectivity issues
# - Topic creation issues
# - Authentication problems
```

### Debug Mode
Enable debug logging for health checks:
```properties
logging.level.com.trackops.server.adapters.output.health=DEBUG
```

## 🔄 Health Check Lifecycle

### Startup Sequence
1. **Application starts** - Basic health check available
2. **Database connection** - Database health check enabled
3. **Redis connection** - Redis health check enabled
4. **Kafka connection** - Kafka health check enabled
5. **Full readiness** - All health checks operational

### Shutdown Sequence
1. **Graceful shutdown** - Health checks remain available
2. **Component shutdown** - Individual health checks fail
3. **Application shutdown** - All health checks unavailable

## 📚 Best Practices

### Health Check Design
- ✅ **Fast execution** - Health checks should complete quickly
- ✅ **Minimal dependencies** - Avoid cascading failures
- ✅ **Meaningful metrics** - Provide actionable information
- ✅ **Graceful degradation** - Handle partial failures

### Monitoring Strategy
- ✅ **Regular monitoring** - Check health status frequently
- ✅ **Alert on failures** - Immediate notification of issues
- ✅ **Trend analysis** - Monitor performance over time
- ✅ **Capacity planning** - Use metrics for scaling decisions

### Operational Procedures
- ✅ **Health check documentation** - Document all endpoints
- ✅ **Runbook procedures** - Define response to failures
- ✅ **Regular testing** - Verify health checks work correctly
- ✅ **Performance tuning** - Optimize health check execution
