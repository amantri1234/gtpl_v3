package com.gtplug.middleware;

import com.gtplug.config.DatabaseConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitMiddleware {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitMiddleware.class);
    
    private final int maxRequests;
    private final long windowMs;
    private final Map<String, RateLimitEntry> rateLimitStore = new ConcurrentHashMap<>();

    public RateLimitMiddleware() {
        this.maxRequests = DatabaseConfig.getRateLimitRequests();
        this.windowMs = DatabaseConfig.getRateLimitWindow();
    }

    public RateLimitMiddleware(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    /**
     * Rate limit for login endpoints
     */
    public Handler loginRateLimit() {
        return createRateLimitHandler("login", 5, 300000); // 5 attempts per 5 minutes
    }

    /**
     * General API rate limit
     */
    public Handler apiRateLimit() {
        return createRateLimitHandler("api", maxRequests, windowMs);
    }

    /**
     * Strict rate limit for sensitive operations
     */
    public Handler strictRateLimit() {
        return createRateLimitHandler("strict", 10, 60000); // 10 per minute
    }

    private Handler createRateLimitHandler(String endpoint, int maxReq, long window) {
        return ctx -> {
            String identifier = getIdentifier(ctx, endpoint);
            long now = System.currentTimeMillis();
            
            RateLimitEntry entry = rateLimitStore.computeIfAbsent(
                identifier, 
                k -> new RateLimitEntry(now)
            );
            
            synchronized (entry) {
                // Reset if window has passed
                if (now - entry.windowStart > window) {
                    entry.reset(now);
                }
                
                // Check if limit exceeded
                if (entry.count >= maxReq) {
                    long retryAfter = (entry.windowStart + window - now) / 1000;
                    logger.warn("Rate limit exceeded for {}: {}", endpoint, identifier);
                    
                    ctx.header("Retry-After", String.valueOf(retryAfter));
                    ctx.status(HttpStatus.TOO_MANY_REQUESTS)
                       .json(new ErrorResponse(
                           "Too many requests. Please try again in " + retryAfter + " seconds.", 
                           429,
                           retryAfter
                       ));
                    return;
                }
                
                entry.increment();
            }
            
            // Clean up old entries periodically
            if (rateLimitStore.size() > 10000) {
                cleanupOldEntries(window);
            }
        };
    }

    private String getIdentifier(Context ctx, String endpoint) {
        String ip = ctx.ip();
        String userAgent = ctx.userAgent() != null ? ctx.userAgent() : "unknown";
        return endpoint + ":" + ip + ":" + userAgent.hashCode();
    }

    private void cleanupOldEntries(long window) {
        long now = System.currentTimeMillis();
        rateLimitStore.entrySet().removeIf(entry -> 
            now - entry.getValue().windowStart > window * 2
        );
    }

    private static class RateLimitEntry {
        int count;
        long windowStart;

        RateLimitEntry(long windowStart) {
            this.windowStart = windowStart;
            this.count = 0;
        }

        synchronized void increment() {
            count++;
        }

        void reset(long newWindowStart) {
            this.windowStart = newWindowStart;
            this.count = 0;
        }
    }

    public static class ErrorResponse {
        public final String error;
        public final int code;
        public final long retryAfter;
        public final long timestamp;

        public ErrorResponse(String error, int code, long retryAfter) {
            this.error = error;
            this.code = code;
            this.retryAfter = retryAfter;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
