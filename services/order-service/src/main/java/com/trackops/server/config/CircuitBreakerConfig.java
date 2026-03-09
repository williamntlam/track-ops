package com.trackops.server.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the "downstreamKafka" circuit breaker as a bean so consumers can inject
 * CircuitBreaker directly without depending on CircuitBreakerRegistry.
 */
@Configuration
@ConditionalOnProperty(name = "app.event-publishing.strategy", havingValue = "debezium")
public class CircuitBreakerConfig {

    public static final String DOWNSTREAM_KAFKA = "downstreamKafka";

    @Bean(name = "downstreamKafkaCircuitBreaker")
    public CircuitBreaker downstreamKafkaCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(DOWNSTREAM_KAFKA);
    }
}
