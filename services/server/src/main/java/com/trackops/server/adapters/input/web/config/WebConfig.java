package com.trackops.server.adapters.input.web.config;

import com.trackops.server.adapters.input.web.interceptors.MetricsInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Web configuration for TrackOps application.
 * Configures interceptors and other web-related settings.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final MetricsInterceptor metricsInterceptor;
    
    @Autowired
    public WebConfig(MetricsInterceptor metricsInterceptor) {
        this.metricsInterceptor = metricsInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add metrics interceptor to all API endpoints
        registry.addInterceptor(metricsInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/actuator/**",  // Exclude actuator endpoints
                    "/health/**",    // Exclude health check endpoints
                    "/error"         // Exclude error pages
                );
    }
}
