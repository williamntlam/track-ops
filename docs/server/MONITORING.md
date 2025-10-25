# TrackOps Monitoring with Prometheus

This document describes the comprehensive monitoring system implemented in TrackOps server using Prometheus, Grafana, and Micrometer.

## 🎯 Monitoring Overview

TrackOps implements a multi-layered monitoring system that includes:
- **Application Metrics** - Business and technical metrics
- **System Metrics** - JVM, database, and infrastructure metrics
- **Custom Metrics** - Order processing, caching, and SAGA metrics
- **Alerting** - Proactive monitoring with alert rules
- **Dashboards** - Real-time visualization with Grafana

## 📊 Available Metrics

### **Order Metrics**
```
# Order creation and processing
orders_created_total                    # Total orders created
orders_status_updated_total            # Total status updates
orders_cancelled_total                 # Total orders cancelled
order_processing_duration_seconds      # Order processing time
orders_by_status_total                 # Orders grouped by status

# Business metrics
active_orders_count                    # Current active orders
total_revenue_cents                    # Total revenue in cents
average_order_value_cents              # Average order value in cents
```

### **Cache Metrics**
```
# Cache performance
cache_hits_total                       # Total cache hits
cache_misses_total                     # Total cache misses
cache_evictions_total                  # Total cache evictions
cache_operation_duration_seconds       # Cache operation time
```

### **Kafka Metrics**
```
# Message processing
kafka_messages_published_total         # Total messages published
kafka_messages_consumed_total          # Total messages consumed
kafka_publish_errors_total             # Total publish errors
kafka_publish_duration_seconds         # Publish operation time
```

### **SAGA Metrics**
```
# SAGA orchestration
saga_instances_created_total           # Total SAGA instances created
saga_instances_completed_total         # Total SAGA instances completed
saga_instances_failed_total            # Total SAGA instances failed
saga_execution_duration_seconds        # SAGA execution time
```

### **API Metrics**
```
# HTTP requests
api_requests_total                     # Total API requests
api_errors_total                       # Total API errors
api_response_duration_seconds          # API response time
```

### **System Metrics (Auto-configured)**
```
# JVM metrics
jvm_memory_used_bytes                  # JVM memory usage
jvm_memory_max_bytes                   # JVM memory max
jvm_gc_pause_seconds                   # GC pause time
jvm_threads_live                       # Live threads

# HTTP server metrics
http_server_requests_seconds           # HTTP request duration
http_server_requests_total             # HTTP request count

# Database metrics
hikaricp_connections_active             # Active connections
hikaricp_connections_idle               # Idle connections
```

## 🚀 Prometheus Endpoints

### **Metrics Endpoint**
```bash
# Prometheus metrics
GET /actuator/prometheus

# Example response
# HELP orders_created_total Total number of orders created
# TYPE orders_created_total counter
orders_created_total{application="trackops-server",environment="default"} 1250

# HELP order_processing_duration_seconds Time taken to process orders
# TYPE order_processing_duration_seconds histogram
order_processing_duration_seconds_bucket{le="0.1"} 800
order_processing_duration_seconds_bucket{le="0.5"} 1200
order_processing_duration_seconds_bucket{le="1.0"} 1250
order_processing_duration_seconds_count 1250
order_processing_duration_seconds_sum 450.5
```

### **Health Endpoints**
```bash
# Application health
GET /actuator/health

# Individual component health
GET /actuator/health/database
GET /actuator/health/redis
GET /actuator/health/kafka
```

## 📈 Grafana Dashboards

### **TrackOps Server Dashboard**
- **API Request Rate** - Requests per second by endpoint
- **API Response Time** - 50th, 95th, 99th percentiles
- **Orders Created** - Order creation rate
- **Cache Hit Rate** - Cache performance
- **JVM Memory Usage** - Memory utilization
- **Kafka Messages** - Message publishing rate
- **SAGA Instances** - SAGA orchestration metrics
- **Business Metrics** - Revenue, active orders, average order value
- **Error Rate** - API error rate

### **System Dashboard**
- **JVM Metrics** - Memory, GC, threads
- **Database Metrics** - Connection pool, query performance
- **Infrastructure** - CPU, memory, disk usage

## 🔔 Alerting Rules

### **Critical Alerts**
```yaml
# Service down
- alert: ServiceDown
  expr: up{job="trackops-server"} == 0
  for: 1m
  severity: critical

# High error rate
- alert: HighErrorRate
  expr: rate(api_errors_total[5m]) > 0.1
  for: 2m
  severity: warning

# High response time
- alert: HighResponseTime
  expr: histogram_quantile(0.95, rate(api_response_duration_seconds_bucket[5m])) > 1
  for: 2m
  severity: warning
```

### **Performance Alerts**
```yaml
# High memory usage
- alert: HighMemoryUsage
  expr: (jvm_memory_used_bytes / jvm_memory_max_bytes) > 0.8
  for: 5m
  severity: warning

# Low cache hit rate
- alert: LowCacheHitRate
  expr: rate(cache_hits_total[5m]) / (rate(cache_hits_total[5m]) + rate(cache_misses_total[5m])) < 0.7
  for: 5m
  severity: warning

# Kafka publish errors
- alert: KafkaPublishErrors
  expr: rate(kafka_publish_errors_total[5m]) > 0.01
  for: 2m
  severity: warning
```

## 🛠️ Setup and Configuration

### **1. Kubernetes Deployment**
```bash
# Deploy Prometheus and Grafana
kubectl apply -f k8s/prometheus-config.yaml

# Deploy TrackOps server
kubectl apply -f k8s/all-services.yaml
```

### **2. Access Dashboards**
```bash
# Prometheus
kubectl port-forward svc/prometheus 9090:9090
open http://localhost:9090

# Grafana
kubectl port-forward svc/grafana 3000:3000
open http://localhost:3000
# Login: admin/admin
```

### **3. Import Grafana Dashboard**
1. Open Grafana at http://localhost:3000
2. Go to "Dashboards" → "Import"
3. Upload `k8s/grafana-dashboard.json`
4. Select Prometheus as data source

## 📊 Key Performance Indicators (KPIs)

### **Application KPIs**
- **Order Processing Rate** - Orders processed per second
- **API Response Time** - 95th percentile response time
- **Error Rate** - Percentage of failed requests
- **Cache Hit Rate** - Percentage of cache hits

### **Business KPIs**
- **Total Revenue** - Cumulative revenue
- **Active Orders** - Current order count
- **Average Order Value** - Mean order value
- **Order Status Distribution** - Orders by status

### **System KPIs**
- **Memory Usage** - JVM memory utilization
- **CPU Usage** - Application CPU consumption
- **Database Connections** - Connection pool usage
- **Kafka Throughput** - Message processing rate

## 🔍 Monitoring Best Practices

### **Metric Naming**
- ✅ **Consistent naming** - Use snake_case for metric names
- ✅ **Descriptive labels** - Include relevant dimensions
- ✅ **Unit suffixes** - Add units to metric names (_total, _seconds, _bytes)

### **Label Usage**
- ✅ **Cardinality control** - Limit high-cardinality labels
- ✅ **Meaningful dimensions** - Include business-relevant labels
- ✅ **Consistent tagging** - Use same labels across related metrics

### **Alerting Strategy**
- ✅ **Threshold-based** - Set appropriate thresholds
- ✅ **Time-based** - Use appropriate time windows
- ✅ **Severity levels** - Critical, warning, info
- ✅ **Runbook integration** - Link alerts to runbooks

### **Dashboard Design**
- ✅ **Business focus** - Show business-relevant metrics
- ✅ **Performance focus** - Include performance indicators
- ✅ **System focus** - Monitor infrastructure health
- ✅ **Alert integration** - Show current alert status

## 🚨 Troubleshooting

### **Common Issues**

#### **Metrics Not Appearing**
```bash
# Check if metrics endpoint is accessible
curl http://localhost:8080/actuator/prometheus

# Check Prometheus targets
curl http://localhost:9090/api/v1/targets
```

#### **High Cardinality**
```bash
# Check for high-cardinality metrics
curl http://localhost:9090/api/v1/query?query=count by (__name__)({__name__=~".+"})
```

#### **Memory Issues**
```bash
# Check JVM memory metrics
curl http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes
```

### **Performance Optimization**
- **Metric sampling** - Reduce metric collection frequency
- **Label filtering** - Remove unnecessary labels
- **Retention policies** - Configure appropriate retention
- **Scraping intervals** - Optimize scrape intervals

## 📚 Advanced Usage

### **Custom Metrics**
```java
// Record custom business metric
@Autowired
private MetricsService metricsService;

public void processOrder(Order order) {
    Timer.Sample sample = metricsService.startOrderProcessingTimer();
    try {
        // Process order
    } finally {
        metricsService.recordOrderProcessingTime(sample);
    }
}
```

### **Metric Queries**
```promql
# Order creation rate
rate(orders_created_total[5m])

# Cache hit rate
rate(cache_hits_total[5m]) / (rate(cache_hits_total[5m]) + rate(cache_misses_total[5m]))

# 95th percentile response time
histogram_quantile(0.95, rate(api_response_duration_seconds_bucket[5m]))

# Memory usage percentage
jvm_memory_used_bytes / jvm_memory_max_bytes
```

### **Alerting Rules**
```yaml
# Custom business alert
- alert: LowOrderVolume
  expr: rate(orders_created_total[1h]) < 0.1
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "Low order volume detected"
    description: "Order creation rate is {{ $value }} orders per second"
```

## 🎯 Production Checklist

### **Monitoring Setup**
- ✅ **Prometheus deployed** - Metrics collection configured
- ✅ **Grafana configured** - Dashboards imported
- ✅ **Alerting configured** - Alert rules active
- ✅ **Notification channels** - Email/Slack integration

### **Metrics Coverage**
- ✅ **Application metrics** - All business processes monitored
- ✅ **System metrics** - JVM and infrastructure covered
- ✅ **Custom metrics** - Business-specific KPIs tracked
- ✅ **Health checks** - All dependencies monitored

### **Alerting Coverage**
- ✅ **Critical alerts** - Service availability
- ✅ **Performance alerts** - Response time and throughput
- ✅ **Business alerts** - Revenue and order metrics
- ✅ **System alerts** - Memory, CPU, disk usage

### **Dashboard Coverage**
- ✅ **Executive dashboard** - High-level business metrics
- ✅ **Operations dashboard** - Technical performance metrics
- ✅ **Development dashboard** - Application-specific metrics
- ✅ **Infrastructure dashboard** - System and infrastructure metrics
