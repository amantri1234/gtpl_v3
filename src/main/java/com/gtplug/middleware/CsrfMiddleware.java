package com.gtplug.middleware;

import com.gtplug.config.AppConfig;
import com.gtplug.security.PasswordUtil;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CsrfMiddleware {
    private static final Logger logger = LoggerFactory.getLogger(CsrfMiddleware.class);
    private static final String CSRF_TOKEN_NAME = "csrf_token";
    private static final String CSRF_HEADER = "X-CSRF-Token";
    private static final long TOKEN_LIFETIME = 3600000; // 1 hour
    
    // In-memory token storage (in production, use Redis or database)
    private final Map<String, CsrfToken> tokenStore = new ConcurrentHashMap<>();

    /**
     * Generate and set CSRF token for the session
     */
    public Handler generateToken() {
        return ctx -> {
            String sessionId = getSessionId(ctx);
            String token = PasswordUtil.generateToken(32);
            
            // Clean expired tokens
            cleanupExpiredTokens();
            
            // Store token
            tokenStore.put(sessionId, new CsrfToken(token, System.currentTimeMillis() + TOKEN_LIFETIME));
            
            // Set token in cookie (not HTTPOnly so JS can read it)
            ctx.cookie(CSRF_TOKEN_NAME, token, (int) (TOKEN_LIFETIME / 1000), false);
            
            // Also set as attribute for templates
            ctx.attribute("csrfToken", token);
        };
    }

    /**
     * Validate CSRF token for state-changing requests
     */
    public Handler validateToken() {
        return ctx -> {
            if (!AppConfig.CSRF_ENABLED) {
                return;
            }
            
            // Only validate for state-changing methods
            String method = ctx.method().name();
            if (method.equals("GET") || method.equals("HEAD") || method.equals("OPTIONS")) {
                return;
            }
            
            // Skip validation for API endpoints that use JWT
            if (ctx.path().startsWith("/api/")) {
                return;
            }

            String sessionId = getSessionId(ctx);
            String submittedToken = getSubmittedToken(ctx);
            CsrfToken storedToken = tokenStore.get(sessionId);
            
            if (storedToken == null || storedToken.isExpired()) {
                logger.warn("CSRF token missing or expired for session: {}", sessionId);
                handleInvalidToken(ctx);
                return;
            }
            
            if (!storedToken.token.equals(submittedToken)) {
                logger.warn("CSRF token mismatch for session: {}", sessionId);
                handleInvalidToken(ctx);
                return;
            }
            
            // Token is valid, consume it (one-time use)
            tokenStore.remove(sessionId);
        };
    }

    /**
     * Refresh CSRF token after successful validation
     */
    public Handler refreshToken() {
        return ctx -> {
            if (!ctx.method().name().equals("GET")) {
                return; // Only refresh on GET requests
            }
            
            String sessionId = getSessionId(ctx);
            CsrfToken existingToken = tokenStore.get(sessionId);
            
            // Generate new token if none exists or about to expire
            if (existingToken == null || existingToken.expiresAt < System.currentTimeMillis() + 300000) {
                String newToken = PasswordUtil.generateToken(32);
                tokenStore.put(sessionId, new CsrfToken(newToken, System.currentTimeMillis() + TOKEN_LIFETIME));
                ctx.cookie(CSRF_TOKEN_NAME, newToken, (int) (TOKEN_LIFETIME / 1000), false);
                ctx.attribute("csrfToken", newToken);
            } else {
                ctx.attribute("csrfToken", existingToken.token);
            }
        };
    }

    private String getSessionId(Context ctx) {
        // Use a combination of IP and user agent for session identification
        // In production, use a proper session ID
        String ip = ctx.ip();
        String userAgent = ctx.userAgent() != null ? ctx.userAgent() : "unknown";
        return java.util.Base64.getEncoder()
            .encodeToString((ip + userAgent).getBytes());
    }

    private String getSubmittedToken(Context ctx) {
        // Check form parameter first
        String formToken = ctx.formParam(CSRF_TOKEN_NAME);
        if (formToken != null && !formToken.isEmpty()) {
            return formToken;
        }
        
        // Check header
        String headerToken = ctx.header(CSRF_HEADER);
        if (headerToken != null && !headerToken.isEmpty()) {
            return headerToken;
        }
        
        return null;
    }

    private void handleInvalidToken(Context ctx) {
        if (isApiRequest(ctx)) {
            ctx.status(HttpStatus.FORBIDDEN)
               .json(new ErrorResponse("Invalid or missing CSRF token", 403));
        } else {
            ctx.status(HttpStatus.FORBIDDEN)
               .render("error/403.html", new java.util.HashMap<>() {{ 
                   put("message", "Invalid or missing security token. Please refresh the page and try again."); 
               }});
        }
    }

    private void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        tokenStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private boolean isApiRequest(Context ctx) {
        String accept = ctx.header("Accept");
        return accept != null && accept.contains("application/json");
    }

    private static class CsrfToken {
        final String token;
        final long expiresAt;

        CsrfToken(String token, long expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    public static class ErrorResponse {
        public final String error;
        public final int code;
        public final long timestamp;

        public ErrorResponse(String error, int code) {
            this.error = error;
            this.code = code;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
