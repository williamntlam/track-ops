# TrackOps Server Logging

This document describes the comprehensive structured logging system implemented in TrackOps server using SLF4J, Logback, and JSON formatting.

## 🎯 Logging Overview

TrackOps implements a multi-layered logging system that includes:
- **Structured JSON Logging** - Machine-readable log format
- **Correlation IDs** - Request tracing across services
- **Business Event Logging** - Order and SAGA event tracking
- **Performance Logging** - API response times and metrics
- **Health Check Logging** - System health monitoring
- **Cache Operation Logging** - Redis cache performance
- **Monitoring Integration** - Logs feed into Prometheus metrics

## 📊 Log Categories

### **1. Application Logs (`trackops-server.log`)**
- **Main application logs** - INFO, DEBUG, WARN, ERROR
- **System events** - Startup, shutdown, configuration
- **General application flow** - Service operations, business logic

### **2. Business Logs (`trackops-server-business.log`)**
- **Order events** - ORDER_CREATED, ORDER_UPDATED, ORDER_CANCELLED
- **SAGA events** - SAGA_STARTED, SAGA_COMPLETED, SAGA_FAILED
- **Customer events** - Customer registration, updates
- **Revenue tracking** - Order values, payment processing

### **3. Access Logs (`trackops-server-access.log`)**
- **API requests** - HTTP method, endpoint, status code
- **Response times** - Request duration in milliseconds
- **Error tracking** - 4xx/5xx errors with details
- **Client information** - IP address, User-Agent

### **4. Error Logs (`trackops-server-error.log`)**
- **Error-only logs** - ERROR level and above
- **Exception details** - Stack traces, error messages
- **System failures** - Database, Redis, Kafka errors

## 🔍 Log Format

### **JSON Structure**
All logs are in structured JSON format for easy parsing and analysis:

```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "logger": "com.trackops.server.application.services.orders.OrderService",
  "message": "Order event: ORDER_CREATED for order 123e4567-e89b-12d3-a456-426614174000 (customer: 987fcdeb-51a2-43d1-b789-123456789abc)",
  "service": "trackops-server",
  "environment": "production",
  "version": "1.0.0",
  "hostname": "trackops-server-1",
  "thread": "http-nio-8080-exec-1",
  "class": "OrderService",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "requestId": "660f9511-f3ac-52e5-b827-557766551111",
  "event": "ORDER_CREATED",
  "entityType": "order",
  "entityId": "123e4567-e89b-12d3-a456-426614174000",
  "customerId": "987fcdeb-51a2-43d1-b789-123456789abc",
  "totalAmount": "89.50",
  "status": "PENDING"
}
```

### **Common Fields**
- **timestamp** - ISO 8601 timestamp
- **level** - Log level (DEBUG, INFO, WARN, ERROR)
- **logger** - Logger name
- **message** - Human-readable log message
- **service** - Application name
- **environment** - Environment (dev, staging, prod)
- **version** - Application version
- **hostname** - Server hostname
- **thread** - Thread name
- **class** - Class name
- **correlationId** - Request correlation ID
- **requestId** - Unique request ID

### **Business Event Fields**
- **event** - Event type (ORDER_CREATED, SAGA_STARTED, etc.)
- **entityType** - Entity type (order, saga, customer)
- **entityId** - Entity identifier
- **customerId** - Customer identifier
- **totalAmount** - Order amount
- **status** - Order status

### **API Access Fields**
- **httpMethod** - HTTP method (GET, POST, PUT, DELETE)
- **endpoint** - API endpoint
- **statusCode** - HTTP status code
- **durationMs** - Request duration in milliseconds
- **userAgent** - Client user agent
- **clientIp** - Client IP address

## 🚀 Correlation IDs

### **Request Tracing**
Every HTTP request gets a unique correlation ID that flows through the entire request lifecycle:

```java
// Correlation ID Filter automatically adds:
MDC.put("correlationId", correlationId);
MDC.put("requestId", requestId);
MDC.put("userAgent", userAgent);
MDC.put("clientIp", clientIp);
```

### **Cross-Service Tracing**
- **X-Correlation-ID header** - Passed between services
- **Request ID** - Unique identifier for each request
- **MDC (Mapped Diagnostic Context)** - Thread-local storage for correlation data

### **Example Flow**
```
1. Client Request → X-Correlation-ID: 550e8400-e29b-41d4-a716-446655440000
2. API Gateway → Adds correlation ID to MDC
3. Order Service → Logs with correlation ID
4. Database → Logs with correlation ID
5. Kafka → Logs with correlation ID
6. Response → Returns correlation ID to client
```

## 📈 Business Event Logging

### **Order Events**
```java
// Order creation
loggingService.logOrderEvent("ORDER_CREATED", orderId, customerId, Map.of(
    "totalAmount", totalAmount,
    "status", "PENDING",
    "deliveryInstructions", instructions
));

// Order status update
loggingService.logOrderEvent("ORDER_UPDATED", orderId, customerId, Map.of(
    "oldStatus", "PENDING",
    "newStatus", "PROCESSING",
    "updatedBy", "system"
));
```

### **SAGA Events**
```java
// SAGA start
loggingService.logSagaEvent("SAGA_STARTED", sagaId, "OrderCancellationSaga", Map.of(
    "orderId", orderId,
    "reason", "customer_request"
));

// SAGA completion
loggingService.logSagaEvent("SAGA_COMPLETED", sagaId, "OrderCancellationSaga", Map.of(
    "orderId", orderId,
    "duration", "2.5s",
    "steps", 3
));
```

### **Cache Events**
```java
// Cache hit
loggingService.logCacheOperation("GET", "order", orderId, true, 5, null);

// Cache miss
loggingService.logCacheOperation("GET", "order", orderId, false, 2, "Key not found");
```

## 🔧 Configuration

### **Logback Configuration**
The logging configuration is in `src/main/resources/logback-spring.xml`:

```xml
<!-- Console Appender for Development -->
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <providers>
            <timestamp/>
            <logLevel/>
            <loggerName/>
            <message/>
            <mdc/>
            <stackTrace/>
        </providers>
    </encoder>
</appender>

<!-- File Appender for Production -->
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_PATH}/${APP_NAME}.log</file>
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <!-- Same encoder configuration -->
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>3GB</totalSizeCap>
    </rollingPolicy>
</appender>
```

### **Environment Variables**
```bash
# Log level (DEBUG, INFO, WARN, ERROR)
export LOG_LEVEL=INFO

# Log directory
export LOG_PATH=logs

# Application version
export APP_VERSION=1.0.0

# Environment
export SPRING_PROFILES_ACTIVE=production
```

### **Profile-specific Configuration**
```xml
<!-- Development Profile -->
<springProfile name="dev,development">
    <logger name="com.trackops.server" level="DEBUG"/>
    <logger name="org.springframework.web" level="DEBUG"/>
</springProfile>

<!-- Production Profile -->
<springProfile name="prod,production">
    <logger name="com.trackops.server" level="INFO"/>
    <logger name="org.springframework" level="WARN"/>
</springProfile>
```

## 📊 Log Analysis

### **Using jq for Log Analysis**
```bash
# Count order creations by hour
cat trackops-server-business.log | jq -r 'select(.event == "ORDER_CREATED") | .timestamp' | cut -d'T' -f1 | sort | uniq -c

# Find slow API requests (>1 second)
cat trackops-server-access.log | jq -r 'select(.durationMs > 1000) | "\(.endpoint) \(.durationMs)ms"'

# Error rate by endpoint
cat trackops-server-access.log | jq -r 'select(.statusCode >= 400) | .endpoint' | sort | uniq -c

# Cache hit rate
cat trackops-server.log | jq -r 'select(.operation == "cache_hit") | .cacheName' | sort | uniq -c

# Revenue tracking
cat trackops-server-business.log | jq -r 'select(.event == "ORDER_CREATED") | .totalAmount' | awk '{sum += $1} END {print "Total Revenue: $" sum}'
```

### **Using ELK Stack**
```yaml
# Logstash configuration
input {
  file {
    path => "/path/to/trackops-server.log"
    codec => "json"
  }
}

filter {
  if [logger] =~ /business/ {
    mutate {
      add_tag => ["business"]
    }
  }
  
  if [logger] =~ /access/ {
    mutate {
      add_tag => ["access"]
    }
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "trackops-logs-%{+YYYY.MM.dd}"
  }
}
```

## 🚨 Monitoring and Alerting

### **Log-based Metrics**
- **Log Volume** - Logs per minute/hour
- **Error Rate** - Error logs vs total logs
- **Response Time** - P95, P99 response times from access logs
- **Business Metrics** - Order creation rate, revenue tracking

### **Alerting Rules**
```yaml
# High error rate
- alert: HighErrorRate
  expr: rate(log_errors_total[5m]) > 0.05
  for: 2m
  labels:
    severity: warning
  annotations:
    summary: "High error rate detected"
    description: "Error rate is {{ $value }} errors per second"

# Slow responses
- alert: SlowResponses
  expr: histogram_quantile(0.95, rate(log_response_time_bucket[5m])) > 1
  for: 2m
  labels:
    severity: warning
  annotations:
    summary: "Slow API responses detected"
    description: "95th percentile response time is {{ $value }} seconds"
```

## 🛠️ Best Practices

### **Logging Guidelines**
- **Use appropriate log levels** - DEBUG for development, INFO for production
- **Include correlation IDs** - For request tracing
- **Log business events** - For analytics and monitoring
- **Avoid sensitive data** - No passwords, tokens, or PII
- **Use structured logging** - JSON format for machine readability

### **Performance Considerations**
- **Asynchronous logging** - For high-throughput scenarios
- **Log rotation** - Prevent disk space issues
- **Filtering** - Reduce log volume with appropriate levels
- **Monitoring** - Track log volume and disk usage

### **Security Considerations**
- **No sensitive data** - Avoid logging passwords, tokens, or PII
- **Access control** - Secure log files and directories
- **Audit logging** - Log security events and access attempts
- **Data retention** - Comply with data protection regulations

## 🔍 Troubleshooting

### **Common Issues**
- **Log files not created** - Check LOG_PATH environment variable
- **High disk usage** - Check log rotation configuration
- **Missing logs** - Check log level configuration
- **Performance impact** - Consider asynchronous logging

### **Debugging Steps**
1. **Check log configuration** - Verify logback-spring.xml
2. **Check environment variables** - LOG_LEVEL, LOG_PATH, APP_VERSION
3. **Check file permissions** - Ensure write access to log directory
4. **Check disk space** - Ensure sufficient space for log files
5. **Check log rotation** - Verify rotation policies are working

### **Log Analysis Tools**
- **jq** - Command-line JSON processor
- **grep** - Text search in log files
- **awk** - Text processing and analysis
- **ELK Stack** - Enterprise log analysis
- **Grafana** - Log visualization and alerting
- **Prometheus** - Log-based metrics and alerting

## 📚 Integration with Monitoring

### **Prometheus Integration**
- **Log-based metrics** - Extract metrics from log events
- **Business metrics** - Order creation rate, revenue tracking
- **Performance metrics** - Response times, error rates
- **Health metrics** - System health from health check logs

### **Grafana Dashboards**
- **Log volume** - Logs per minute/hour
- **Error rate** - Error logs vs total logs
- **Response time** - P95, P99 response times
- **Business metrics** - Order creation, revenue tracking
- **System health** - Health check status and performance

### **Alerting Integration**
- **Log-based alerts** - High error rate, slow responses
- **Business alerts** - Unusual order patterns, revenue drops
- **System alerts** - Health check failures, disk space issues
- **Security alerts** - Unauthorized access, suspicious activity
