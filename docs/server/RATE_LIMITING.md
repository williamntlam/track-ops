# Rate Limiting Implementation

## Overview

Rate limiting has been implemented using **Bucket4j** with **Redis** for distributed rate limiting across multiple service instances. This protects the API from abuse and ensures fair usage.

## Features

- ✅ **Redis-based distributed rate limiting** - Works across multiple service instances
- ✅ **Per-client rate limits** - Based on IP address or API key
- ✅ **Configurable limits** - Default: 1000 requests per hour with 100 burst capacity
- ✅ **Rate limit headers** - Standard headers included in all responses
- ✅ **Fail-open behavior** - Allows requests if Redis is unavailable (logs error)
- ✅ **Actuator endpoints excluded** - Health checks and metrics are not rate limited

## Configuration

Rate limiting can be configured via `application.properties`:

```properties
# Enable/disable rate limiting
app.rate-limit.enabled=true

# Default requests allowed per hour per client
app.rate-limit.default-requests-per-hour=1000

# Burst capacity (initial tokens available)
app.rate-limit.burst-capacity=100
```

## How It Works

1. **Client Identification**: The system identifies clients by:
   - API Key (if `X-API-Key` header is present)
   - IP Address (fallback, considers `X-Forwarded-For` and `X-Real-IP` headers)

2. **Rate Limit Check**: Each request is checked against Redis using a token bucket algorithm:
   - Tokens are refilled at a rate of `default-requests-per-hour` per hour
   - Initial burst capacity allows `burst-capacity` requests immediately
   - Each request consumes 1 token

3. **Response Headers**: All responses include rate limit information:
   - `X-RateLimit-Limit`: Maximum requests allowed per hour
   - `X-RateLimit-Remaining`: Remaining requests in current window
   - `X-RateLimit-Reset`: Unix timestamp when the limit resets

4. **Rate Limit Exceeded**: When limit is exceeded:
   - HTTP 429 (Too Many Requests) status code
   - JSON error response with details
   - Rate limit headers still included

## Example Response Headers

```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1642248000
```

## Example Error Response (429)

```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Limit: 1000 requests per hour. Reset at: 1642248000"
}
```

## Architecture

The implementation consists of:

1. **RateLimitConfig** (`com.trackops.server.config.RateLimitConfig`)
   - Configures Redis connection for rate limiting
   - Creates `ProxyManager` for distributed rate limiting
   - Defines rate limit properties

2. **RateLimitService** (`com.trackops.server.application.services.ratelimit.RateLimitService`)
   - Core business logic for rate limit checking
   - Uses Bucket4j with Redis backend
   - Returns rate limit results with metadata

3. **RateLimitFilter** (`com.trackops.server.adapters.input.web.filters.RateLimitFilter`)
   - Servlet filter that intercepts all requests
   - Executes after `CorrelationIdFilter` (Order 2)
   - Skips actuator endpoints
   - Adds rate limit headers to responses

## Dependencies

The following dependencies were added to `build.gradle`:

```gradle
// Bucket4j for rate limiting (Java 21 compatible with JDK 17 artifacts)
implementation 'com.bucket4j:bucket4j_jdk17-core:8.10.1'
implementation 'com.bucket4j:bucket4j_jdk17-redis-common:8.10.1'
implementation 'com.bucket4j:bucket4j_jdk17-lettuce:8.10.1'
```

## Testing

To test rate limiting:

1. **Normal Request**:
   ```bash
   curl -X GET http://localhost:8081/api/orders
   ```
   Check response headers for rate limit information.

2. **Rate Limit Exceeded**:
   ```bash
   # Make 1001 requests quickly
   for i in {1..1001}; do
     curl -X GET http://localhost:8081/api/orders
   done
   ```
   The 1001st request should return 429.

3. **With API Key** (if implemented):
   ```bash
   curl -X GET http://localhost:8081/api/orders \
     -H "X-API-Key: your-api-key"
   ```

## Customization

### Per-Endpoint Rate Limits

To implement different rate limits for different endpoints, you can:

1. Extend `RateLimitService` to accept endpoint-specific limits
2. Modify `RateLimitFilter` to check the request path
3. Use different bucket configurations based on endpoint

### Per-Client Tier Limits

To implement tiered rate limits (e.g., Standard, Premium, Enterprise):

1. Create a service to look up client tier from API key or database
2. Modify `RateLimitService.checkRateLimit()` to accept tier information
3. Use different bucket configurations based on tier

## Monitoring

Rate limiting events are logged:

- **DEBUG**: Successful rate limit checks with remaining tokens
- **WARN**: Rate limit exceeded
- **ERROR**: Redis connection failures (fail-open behavior)

Monitor these logs to track rate limiting behavior and identify potential issues.

## Production Considerations

1. **Redis High Availability**: Ensure Redis is highly available to prevent rate limiting failures
2. **Monitoring**: Monitor rate limit hit rates and adjust limits as needed
3. **Scaling**: Rate limits are shared across all service instances via Redis
4. **Performance**: Rate limit checks add minimal latency (~1-2ms with local Redis)

## Disabling Rate Limiting

To disable rate limiting (e.g., for development):

```properties
app.rate-limit.enabled=false
```

When disabled, all requests are allowed and no rate limit headers are added.
