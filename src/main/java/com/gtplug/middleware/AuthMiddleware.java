package com.gtplug.middleware;

import com.gtplug.security.AuthContext;
import com.gtplug.security.JwtUtil;
import com.gtplug.services.UserService;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import io.jsonwebtoken.Claims;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthMiddleware {
    private static final Logger logger = LoggerFactory.getLogger(AuthMiddleware.class);
    private static final String AUTH_COOKIE = "auth_token";
    private final UserService userService;

    public AuthMiddleware(UserService userService) {
        this.userService = userService;
    }

    /**
     * Handler to authenticate requests via JWT cookie
     */
    public Handler authenticate() {
        return ctx -> {
            String token = getTokenFromCookie(ctx);
            
            if (token == null) {
                logger.debug("No auth token found in request");
                handleUnauthorized(ctx, "Authentication required");
                return;
            }

            try {
                Claims claims = JwtUtil.validateToken(token);
                Long userId = Long.parseLong(claims.getSubject());
                String username = claims.get("username", String.class);
                String roleStr = claims.get("role", String.class);
                
                // Verify user still exists and is active
                var userOpt = userService.findById(userId);
                if (userOpt.isEmpty()) {
                    logger.warn("User {} not found in database", userId);
                    handleUnauthorized(ctx, "Invalid authentication");
                    return;
                }
                
                var user = userOpt.get();
                if (!user.isActive()) {
                    logger.warn("User {} account is deactivated", userId);
                    handleUnauthorized(ctx, "Account is deactivated");
                    return;
                }
                
                if (user.isLocked()) {
                    logger.warn("User {} account is locked", userId);
                    handleUnauthorized(ctx, "Account is temporarily locked");
                    return;
                }

                // Set authentication context
                AuthContext authContext = new AuthContext(
                    userId, 
                    username, 
                    com.gtplug.models.User.UserRole.valueOf(roleStr),
                    token
                );
                AuthContext.setCurrent(authContext);
                
                // Add user info to context attribute for controllers
                ctx.attribute("currentUser", user);
                ctx.attribute("authContext", authContext);
                
            } catch (SecurityException e) {
                logger.warn("Token validation failed: {}", e.getMessage());
                clearAuthCookie(ctx);
                handleUnauthorized(ctx, "Invalid or expired token");
            }
        };
    }

    /**
     * Handler to require admin role
     */
    public Handler requireAdmin() {
        return ctx -> {
            AuthContext authCtx = AuthContext.getCurrent();
            if (authCtx == null || !authCtx.isAdmin()) {
                logger.warn("Admin access denied for user: {}", 
                    authCtx != null ? authCtx.getUsername() : "anonymous");
                handleForbidden(ctx, "Admin access required");
            }
        };
    }

    /**
     * Handler to require vendor role
     */
    public Handler requireVendor() {
        return ctx -> {
            AuthContext authCtx = AuthContext.getCurrent();
            if (authCtx == null || !authCtx.isVendor()) {
                logger.warn("Vendor access denied for user: {}", 
                    authCtx != null ? authCtx.getUsername() : "anonymous");
                handleForbidden(ctx, "Vendor access required");
            }
        };
    }

    /**
     * Handler to clear auth context after request
     */
    public Handler clearContext() {
        return ctx -> AuthContext.clear();
    }

    private String getTokenFromCookie(Context ctx) {
        var cookie = ctx.cookie(AUTH_COOKIE);
        if (cookie != null && !cookie.isEmpty()) {
            return cookie;
        }
        
        // Also check Authorization header as fallback
        String authHeader = ctx.header("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }

    public static void setAuthCookie(Context ctx, String token) {
        // HTTPOnly cookie for security
        ctx.cookie(AUTH_COOKIE, token, (int) (JwtUtil.getExpirationTime() / 1000), true);
    }

    public static void clearAuthCookie(Context ctx) {
        ctx.removeCookie(AUTH_COOKIE);
    }

    private void handleUnauthorized(Context ctx, String message) {
        if (isApiRequest(ctx)) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(new ErrorResponse(message, 401));
        } else {
            ctx.redirect("/login?error=" + message.replace(" ", "+"));
        }
    }

    private void handleForbidden(Context ctx, String message) {
        if (isApiRequest(ctx)) {
            ctx.status(HttpStatus.FORBIDDEN)
               .json(new ErrorResponse(message, 403));
        } else {
            ctx.status(HttpStatus.FORBIDDEN)
               .render("error/403.html", new java.util.HashMap<>() {{ put("message", message); }});
        }
    }

    private boolean isApiRequest(Context ctx) {
        String accept = ctx.header("Accept");
        String contentType = ctx.header("Content-Type");
        String path = ctx.path();
        
        return (accept != null && accept.contains("application/json")) ||
               (contentType != null && contentType.contains("application/json")) ||
               path.startsWith("/api/");
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
