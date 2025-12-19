package com.trackops.server.adapters.input.web.filters;

import com.trackops.server.application.services.ratelimit.RateLimitService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Filter to enforce rate limiting on API requests.
 * Uses Redis-based distributed rate limiting to work across multiple service instances.
 * 
 * Rate limit headers are added to all responses:
 * - X-RateLimit-Limit: Maximum number of requests allowed
 * - X-RateLimit-Remaining: Number of requests remaining in the current window
 * - X-RateLimit-Reset: Unix timestamp when the rate limit resets
 */
@Component
@Order(2) // Execute after CorrelationIdFilter but before security
public class RateLimitFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    
    private static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";
    private static final String API_KEY_HEADER = "X-API-Key";
    
    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip rate limiting for actuator endpoints
        String requestPath = httpRequest.getRequestURI();
        if (requestPath.startsWith("/actuator") || requestPath.equals("/")) {
            chain.doFilter(request, response);
            return;
        }

        // Extract client identifier (API key or IP address)
        String clientId = extractClientId(httpRequest);
        
        // Check rate limit
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(clientId);
        
        // Add rate limit headers to response
        httpResponse.setHeader(RATE_LIMIT_LIMIT_HEADER, String.valueOf(result.getLimit()));
        httpResponse.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(result.getRemaining()));
        httpResponse.setHeader(RATE_LIMIT_RESET_HEADER, String.valueOf(result.getResetTime()));
        
        // If rate limit exceeded, return 429 Too Many Requests
        if (!result.isAllowed()) {
            logger.warn("Rate limit exceeded for client: {}, path: {}", clientId, requestPath);
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                String.format(
                    "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Limit: %d requests per hour. Reset at: %d\"}",
                    result.getLimit(),
                    result.getResetTime()
                )
            );
            return;
        }
        
        // Continue with the filter chain
        chain.doFilter(request, response);
    }

    /**
     * Extracts client identifier from request.
     * Priority: API Key > X-Forwarded-For > Remote Address
     */
    private String extractClientId(HttpServletRequest request) {
        // Check for API key first (if implemented)
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (StringUtils.hasText(apiKey)) {
            return "api_key:" + apiKey;
        }
        
        // Fall back to IP address
        String clientIp = getClientIpAddress(request);
        return "ip:" + clientIp;
    }

    /**
     * Extracts client IP address from request, considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
