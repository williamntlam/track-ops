package com.trackops.server.adapters.input.web.interceptors;

import com.trackops.server.adapters.output.monitoring.MetricsService;
import com.trackops.server.adapters.output.logging.StructuredLoggingService;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor to automatically record API metrics for all HTTP requests.
 * Records request counts, response times, and error rates.
 */
@Component
public class MetricsInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsInterceptor.class);
    private final MetricsService metricsService;
    private final StructuredLoggingService loggingService;
    private static final String TIMER_ATTRIBUTE = "metrics.timer.sample";
    private static final String START_TIME_ATTRIBUTE = "metrics.start.time";
    
    @Autowired
    public MetricsInterceptor(MetricsService metricsService, StructuredLoggingService loggingService) {
        this.metricsService = metricsService;
        this.loggingService = loggingService;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Start timer for this request
        Timer.Sample sample = metricsService.startApiResponseTimer();
        request.setAttribute(TIMER_ATTRIBUTE, sample);
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        
        // Record API request
        String method = request.getMethod();
        String endpoint = getEndpointFromRequest(request);
        metricsService.recordApiRequest(method, endpoint);
        
        logger.debug("API request started: {} {}", method, endpoint);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        // Get timer sample and start time from request
        Timer.Sample sample = (Timer.Sample) request.getAttribute(TIMER_ATTRIBUTE);
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        
        String method = request.getMethod();
        String endpoint = getEndpointFromRequest(request);
        int statusCode = response.getStatus();
        long durationMs = startTime != null ? System.currentTimeMillis() - startTime : 0;
        
        // Record response time
        if (sample != null) {
            metricsService.recordApiResponseTime(sample, method, endpoint);
        }
        
        // Record API error if status indicates error
        if (statusCode >= 400) {
            String error = "HTTP_" + statusCode;
            metricsService.recordApiError(method, endpoint, error);
            
            // Log API error
            String userAgent = request.getHeader("User-Agent");
            String clientIp = getClientIpAddress(request);
            loggingService.logApiError(method, endpoint, statusCode, 
                ex != null ? ex.getMessage() : "HTTP " + statusCode, userAgent, clientIp);
        } else {
            // Log successful API access
            String userAgent = request.getHeader("User-Agent");
            String clientIp = getClientIpAddress(request);
            loggingService.logApiAccess(method, endpoint, statusCode, durationMs, userAgent, clientIp);
        }
        
        logger.debug("API request completed: {} {} -> {} ({}ms)", method, endpoint, statusCode, durationMs);
    }
    
    /**
     * Extract a clean endpoint name from the request.
     * Converts path parameters to placeholders for better metric grouping.
     */
    private String getEndpointFromRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Remove query parameters
        if (path.contains("?")) {
            path = path.substring(0, path.indexOf("?"));
        }
        
        // Convert UUIDs and IDs to placeholders
        path = path.replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "/{id}");
        path = path.replaceAll("/[0-9]+", "/{id}");
        
        // Convert other common path parameters
        path = path.replaceAll("/[A-Z_]+", "/{status}");
        
        return path;
    }
    
    /**
     * Extract client IP address from request, considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
