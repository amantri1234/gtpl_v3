package com.gtplug.security;

import com.gtplug.models.User;

/**
 * Thread-local storage for authentication context
 */
public class AuthContext {
    private static final ThreadLocal<AuthContext> current = new ThreadLocal<>();
    
    private final Long userId;
    private final String username;
    private final User.UserRole role;
    private final String token;

    public AuthContext(Long userId, String username, User.UserRole role, String token) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.token = token;
    }

    public static void setCurrent(AuthContext context) {
        current.set(context);
    }

    public static AuthContext getCurrent() {
        return current.get();
    }

    public static void clear() {
        current.remove();
    }

    public static boolean isAuthenticated() {
        return current.get() != null;
    }

    public static boolean isAdmin() {
        AuthContext ctx = current.get();
        return ctx != null && ctx.role == User.UserRole.ADMIN;
    }

    public static boolean isVendor() {
        AuthContext ctx = current.get();
        return ctx != null && ctx.role == User.UserRole.VENDOR;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public User.UserRole getRole() { return role; }
    public String getToken() { return token; }

    public boolean hasRole(User.UserRole requiredRole) {
        return this.role == requiredRole;
    }
}
