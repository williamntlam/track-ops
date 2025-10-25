# TrackOps Server Deployment Guide

This guide covers deploying the TrackOps server in various environments, from local development to production.

## 📋 Prerequisites

### System Requirements
- **Java 17+** - OpenJDK or Oracle JDK
- **Docker** - For containerized deployment
- **Docker Compose** - For local development
- **Kubernetes** - For production deployment (optional)

### External Dependencies
- **PostgreSQL 13+** - Primary database
- **Redis 6+** - Caching layer
- **Apache Kafka 2.8+** - Event streaming

## 🚀 Local Development

### 1. Using Docker Compose

Create a `docker-compose.yml` file:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: trackops
      POSTGRES_USER: trackops
      POSTGRES_PASSWORD: trackops123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    volumes:
      - zookeeper_data:/var/lib/zookeeper/data

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: true
    volumes:
      - kafka_data:/var/lib/kafka/data

  trackops-server:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/trackops
      SPRING_DATASOURCE_USERNAME: trackops
      SPRING_DATASOURCE_PASSWORD: trackops123
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      - postgres
      - redis
      - kafka

volumes:
  postgres_data:
  redis_data:
  zookeeper_data:
  kafka_data:
```

### 2. Start Services

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f trackops-server

# Stop services
docker-compose down
```

### 3. Application Configuration

Create `application-docker.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/trackops
    username: trackops
    password: trackops123
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  redis:
    host: redis
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
  
  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      group-id: trackops-orders
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

logging:
  level:
    com.trackops: DEBUG
    org.springframework.kafka: INFO
```

## 🐳 Docker Deployment

### 1. Create Dockerfile

```dockerfile
# Multi-stage build for optimized image
FROM openjdk:17-jdk-slim as builder

WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle gradlew ./
COPY gradle/ gradle/

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build application
RUN ./gradlew build --no-daemon -x test

# Runtime stage
FROM openjdk:17-jre-slim

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy built application
COPY --from=builder /app/build/libs/*.jar app.jar

# Create non-root user
RUN groupadd -r trackops && useradd -r -g trackops trackops
RUN chown -R trackops:trackops /app
USER trackops

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 2. Build and Run

```bash
# Build image
docker build -t trackops-server:latest .

# Run container
docker run -d \
  --name trackops-server \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/trackops \
  -e SPRING_DATASOURCE_USERNAME=trackops \
  -e SPRING_DATASOURCE_PASSWORD=your-password \
  -e SPRING_REDIS_HOST=redis \
  -e SPRING_REDIS_PORT=6379 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  trackops-server:latest
```

## ☸️ Kubernetes Deployment

### 1. Namespace

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: trackops
```

### 2. ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: trackops-config
  namespace: trackops
data:
  application.yml: |
    spring:
      datasource:
        url: jdbc:postgresql://postgres-service:5432/trackops
        username: trackops
        password: ${DB_PASSWORD}
        driver-class-name: org.postgresql.Driver
      
      redis:
        host: redis-service
        port: 6379
        timeout: 2000ms
      
      kafka:
        bootstrap-servers: kafka-service:9092
        consumer:
          group-id: trackops-orders
          auto-offset-reset: earliest
        producer:
          acks: all
          retries: 3
    
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      endpoint:
        health:
          show-details: always
```

### 3. Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: trackops-secrets
  namespace: trackops
type: Opaque
data:
  DB_PASSWORD: <base64-encoded-password>
  API_KEY: <base64-encoded-api-key>
```

### 4. Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: trackops-server
  namespace: trackops
spec:
  replicas: 3
  selector:
    matchLabels:
      app: trackops-server
  template:
    metadata:
      labels:
        app: trackops-server
    spec:
      containers:
      - name: trackops-server
        image: trackops-server:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: trackops-secrets
              key: DB_PASSWORD
        - name: API_KEY
          valueFrom:
            secretKeyRef:
              name: trackops-secrets
              key: API_KEY
        volumeMounts:
        - name: config
          mountPath: /app/config
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
      volumes:
      - name: config
        configMap:
          name: trackops-config
```

### 5. Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: trackops-service
  namespace: trackops
spec:
  selector:
    app: trackops-server
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP
```

### 6. Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: trackops-ingress
  namespace: trackops
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  tls:
  - hosts:
    - api.trackops.com
    secretName: trackops-tls
  rules:
  - host: api.trackops.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: trackops-service
            port:
              number: 80
```

## 🌍 Environment Configurations

### Development

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/trackops_dev
    username: trackops
    password: dev123
  
  redis:
    host: localhost
    port: 6379
  
  kafka:
    bootstrap-servers: localhost:9092

logging:
  level:
    com.trackops: DEBUG
    org.springframework: INFO
```

### Staging

```yaml
# application-staging.yml
spring:
  datasource:
    url: jdbc:postgresql://staging-db:5432/trackops_staging
    username: trackops
    password: ${DB_PASSWORD}
  
  redis:
    host: staging-redis
    port: 6379
  
  kafka:
    bootstrap-servers: staging-kafka:9092

logging:
  level:
    com.trackops: INFO
    org.springframework: WARN
```

### Production

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://prod-db:5432/trackops_prod
    username: trackops
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  
  redis:
    host: prod-redis
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
  
  kafka:
    bootstrap-servers: prod-kafka:9092
    consumer:
      group-id: trackops-orders-prod
      auto-offset-reset: latest
    producer:
      acks: all
      retries: 3
      batch-size: 16384
      linger-ms: 5

logging:
  level:
    com.trackops: WARN
    org.springframework: ERROR
    root: WARN
```

## 🔧 Configuration Management

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SPRING_PROFILES_ACTIVE` | Active profiles | `default` | No |
| `SPRING_DATASOURCE_URL` | Database URL | - | Yes |
| `SPRING_DATASOURCE_USERNAME` | Database username | - | Yes |
| `SPRING_DATASOURCE_PASSWORD` | Database password | - | Yes |
| `SPRING_REDIS_HOST` | Redis host | `localhost` | No |
| `SPRING_REDIS_PORT` | Redis port | `6379` | No |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka servers | `localhost:9092` | No |
| `SERVER_PORT` | Application port | `8080` | No |
| `JAVA_OPTS` | JVM options | - | No |

### External Configuration

```yaml
# application.yml
spring:
  config:
    import: "optional:configserver:http://config-server:8888"
  
  cloud:
    config:
      uri: http://config-server:8888
      name: trackops-server
      profile: ${SPRING_PROFILES_ACTIVE}
```

## 📊 Monitoring & Health Checks

### Health Check Endpoints

```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db

# Redis health
curl http://localhost:8080/actuator/health/redis

# Kafka health
curl http://localhost:8080/actuator/health/kafka
```

### Metrics Endpoints

```bash
# Application metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

## 🔒 Security Configuration

### SSL/TLS

```yaml
# application-prod.yml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: trackops
```

### API Security

```yaml
# application-prod.yml
trackops:
  security:
    api-key:
      enabled: true
      header-name: X-API-Key
      valid-keys: ${API_KEYS}
    
    rate-limiting:
      enabled: true
      requests-per-minute: 1000
      burst-capacity: 2000
```

## 🚀 CI/CD Pipeline

### GitHub Actions

```yaml
# .github/workflows/deploy.yml
name: Deploy TrackOps Server

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Run tests
      run: ./gradlew test
    
    - name: Build application
      run: ./gradlew build -x test

  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
    - uses: actions/checkout@v3
    
    - name: Build Docker image
      run: docker build -t trackops-server:${{ github.sha }} .
    
    - name: Push to registry
      run: |
        docker tag trackops-server:${{ github.sha }} ${{ secrets.REGISTRY }}/trackops-server:${{ github.sha }}
        docker push ${{ secrets.REGISTRY }}/trackops-server:${{ github.sha }}

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/trackops-server trackops-server=${{ secrets.REGISTRY }}/trackops-server:${{ github.sha }}
        kubectl rollout status deployment/trackops-server
```

## 🔄 Database Migrations

### Flyway Configuration

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
```

### Migration Files

```sql
-- V1__Create_orders_table.sql
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    country VARCHAR(50) NOT NULL,
    delivery_instructions VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL
);

-- V2__Create_outbox_events_table.sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);
```

## 🛠️ Troubleshooting

### Common Issues

#### 1. Database Connection Issues
```bash
# Check database connectivity
docker exec -it trackops-server curl -f http://localhost:8080/actuator/health/db

# Check database logs
docker logs postgres
```

#### 2. Redis Connection Issues
```bash
# Check Redis connectivity
docker exec -it trackops-server curl -f http://localhost:8080/actuator/health/redis

# Check Redis logs
docker logs redis
```

#### 3. Kafka Connection Issues
```bash
# Check Kafka connectivity
docker exec -it trackops-server curl -f http://localhost:8080/actuator/health/kafka

# Check Kafka logs
docker logs kafka
```

#### 4. Application Startup Issues
```bash
# Check application logs
docker logs trackops-server

# Check JVM memory
docker exec -it trackops-server curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

### Performance Tuning

#### JVM Tuning
```bash
# Production JVM options
JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxGCPauseMillis=200"
```

#### Database Tuning
```sql
-- PostgreSQL configuration
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET default_statistics_target = 100;
```

#### Redis Tuning
```bash
# Redis configuration
maxmemory 512mb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
```

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Docker Documentation](https://docs.docker.com/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/documentation)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
