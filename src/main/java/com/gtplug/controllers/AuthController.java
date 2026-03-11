package com.gtplug.controllers;

import com.gtplug.middleware.AuthMiddleware;
import com.gtplug.models.User;
import com.gtplug.security.JwtUtil;
import com.gtplug.services.UserService;
import com.gtplug.services.VendorService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final VendorService vendorService;

    public AuthController() {
        this.userService = new UserService();
        this.vendorService = new VendorService();
    }

    public void showLogin(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        String error = ctx.queryParam("error");
        if (error != null) {
            model.put("error", error);
        }
        String logout = ctx.queryParam("logout");
        if (logout != null) {
            model.put("message", "You have been logged out successfully");
        }
        ctx.render("auth/login.html", model);
    }

    public void doLogin(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");
        String role = ctx.formParam("role");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            ctx.redirect("/login?error=Username+and+password+are+required");
            return;
        }

        var result = userService.authenticate(username, password);

        if (!result.isSuccess()) {
            ctx.redirect("/login?error=" + result.getMessage().replace(" ", "+"));
            return;
        }

        User user = result.getUser();

        // Check role
        if (role != null && !role.isEmpty()) {
            if (role.equals("admin") && user.getRole() != User.UserRole.ADMIN) {
                ctx.redirect("/login?error=Invalid+role+selected");
                return;
            }
            if (role.equals("vendor") && user.getRole() != User.UserRole.VENDOR) {
                ctx.redirect("/login?error=Invalid+role+selected");
                return;
            }
        }

        // Generate JWT and set cookie
        String token = JwtUtil.generateToken(user);
        AuthMiddleware.setAuthCookie(ctx, token);

        // Redirect based on role
        if (user.getRole() == User.UserRole.ADMIN) {
            ctx.redirect("/admin/dashboard");
        } else {
            ctx.redirect("/vendor/dashboard");
        }
    }

    public void showVendorSignup(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        String error = ctx.queryParam("error");
        if (error != null) {
            model.put("error", error);
        }
        ctx.render("auth/vendor-signup.html", model);
    }

    public void doVendorSignup(Context ctx) {
        String vendorName = ctx.formParam("vendorName");
        String username = ctx.formParam("username");
        String email = ctx.formParam("email");
        String mobileNumber = ctx.formParam("mobileNumber");
        String password = ctx.formParam("password");
        String confirmPassword = ctx.formParam("confirmPassword");

        // Validate inputs
        if (vendorName == null || username == null || email == null || 
            mobileNumber == null || password == null ||
            vendorName.isEmpty() || username.isEmpty() || email.isEmpty() || 
            mobileNumber.isEmpty() || password.isEmpty()) {
            ctx.redirect("/vendor/signup?error=All+fields+are+required");
            return;
        }

        if (!password.equals(confirmPassword)) {
            ctx.redirect("/vendor/signup?error=Passwords+do+not+match");
            return;
        }

        // Register vendor
        var result = vendorService.registerVendor(vendorName, username, email, mobileNumber, password);

        if (!result.isSuccess()) {
            ctx.redirect("/vendor/signup?error=" + result.getMessage().replace(" ", "+"));
            return;
        }

        logger.info("Vendor registered: {}", username);
        ctx.redirect("/login?registered=true");
    }

    public void doLogout(Context ctx) {
        AuthMiddleware.clearAuthCookie(ctx);
        ctx.redirect("/login?logout=true");
    }
}
