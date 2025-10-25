package com.trackops.server.adapters.input.web.interceptors;

import com.trackops.server.adapters.output.monitoring.MetricsService;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor to automatically record API metrics for all HTTP requests.
 * Records request counts, response times, and error rates.
 */
@Component
public class MetricsInterceptor implements HandlerInterceptor {
    
    private final MetricsService metricsService;
    private static final String TIMER_ATTRIBUTE = "metrics.timer.sample";
    
    @Autowired
    public MetricsInterceptor(MetricsService metricsService) {
        this.metricsService = metricsService;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Start timer for this request
        Timer.Sample sample = metricsService.startApiResponseTimer();
        request.setAttribute(TIMER_ATTRIBUTE, sample);
        
        // Record API request
        String method = request.getMethod();
        String endpoint = getEndpointFromRequest(request);
        metricsService.recordApiRequest(method, endpoint);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        // Get timer sample from request
        Timer.Sample sample = (Timer.Sample) request.getAttribute(TIMER_ATTRIBUTE);
        if (sample != null) {
            // Record response time
            String method = request.getMethod();
            String endpoint = getEndpointFromRequest(request);
            metricsService.recordApiResponseTime(sample, method, endpoint);
        }
        
        // Record API error if status indicates error
        if (response.getStatus() >= 400) {
            String method = request.getMethod();
            String endpoint = getEndpointFromRequest(request);
            String error = "HTTP_" + response.getStatus();
            metricsService.recordApiError(method, endpoint, error);
        }
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
}
