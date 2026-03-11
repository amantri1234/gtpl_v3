package com.gtplug;

import com.gtplug.config.AppConfig;
import com.gtplug.config.DatabaseConfig;
import com.gtplug.controllers.*;
import com.gtplug.middleware.AuthMiddleware;
import com.gtplug.middleware.CsrfMiddleware;
import com.gtplug.middleware.RateLimitMiddleware;
import com.gtplug.services.UserService;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        // Initialize Thymeleaf
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(Boolean.parseBoolean(System.getenv().getOrDefault("THYMELEAF_CACHE", "false")));
        templateEngine.setTemplateResolver(resolver);

        // Create Javalin app
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.staticFiles.add(staticConfig -> {
                staticConfig.directory = "/static";
                staticConfig.location = Location.CLASSPATH;
                staticConfig.precompress = true;
            });
            config.fileRenderer(new JavalinThymeleaf(templateEngine));
            
            // Security headers
            config.plugins.enableCors(cors -> {
                cors.add(corsConfig -> {
                    corsConfig.allowHost("http://localhost:8080");
                    corsConfig.allowHost("https://*");
                });
            });
        });

        // Initialize services
        UserService userService = new UserService();

        // Initialize middleware
        AuthMiddleware authMiddleware = new AuthMiddleware(userService);
        CsrfMiddleware csrfMiddleware = new CsrfMiddleware();
        RateLimitMiddleware rateLimitMiddleware = new RateLimitMiddleware();

        // Initialize controllers
        AuthController authController = new AuthController();
        AdminController adminController = new AdminController();
        VendorController vendorController = new VendorController();
        PublicController publicController = new PublicController();

        // Setup routes
        setupRoutes(app, authMiddleware, csrfMiddleware, rateLimitMiddleware, 
            authController, adminController, vendorController, publicController);

        // Setup error handlers
        setupErrorHandlers(app);

        // Setup shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down application...");
            DatabaseConfig.closeDataSource();
        }));

        // Start server
        int port = AppConfig.SERVER_PORT;
        app.start(port);
        logger.info("{} v{} started on port {}", AppConfig.APP_NAME, AppConfig.APP_VERSION, port);
    }

    private static void setupRoutes(Javalin app, AuthMiddleware authMiddleware, 
                                     CsrfMiddleware csrfMiddleware, RateLimitMiddleware rateLimitMiddleware,
                                     AuthController auth, AdminController admin, VendorController vendor,
                                     PublicController pub) {
        
        // Health check
        app.get("/health", ctx -> ctx.json(new HealthResponse("UP", AppConfig.APP_VERSION)));

        // CSRF token generation for all routes
        app.before(csrfMiddleware.generateToken());
        app.before(csrfMiddleware.refreshToken());

        // Public routes
        app.get("/", pub::home);
        app.get("/login", auth::showLogin);
        app.post("/login", rateLimitMiddleware.loginRateLimit(), auth::doLogin);
        app.get("/vendor/signup", auth::showVendorSignup);
        app.post("/vendor/signup", rateLimitMiddleware.loginRateLimit(), auth::doVendorSignup);
        app.get("/logout", auth::doLogout);

        // PWA routes
        app.get("/manifest.json", pub::manifest);
        app.get("/service-worker.js", pub::serviceWorker);

        // Protected routes - require authentication
        app.before("/admin/*", authMiddleware.authenticate());
        app.before("/admin/*", authMiddleware.requireAdmin());
        app.before("/vendor/dashboard/*", authMiddleware.authenticate());
        app.before("/vendor/dashboard/*", authMiddleware.requireVendor());

        // Admin routes
        app.get("/admin/dashboard", admin::dashboard);
        app.get("/admin/vendors", admin::vendorCatalog);
        app.get("/admin/vendors/search", admin::searchVendors);
        app.get("/admin/assign-work", admin::showAssignWork);
        app.post("/admin/assign-work", csrfMiddleware.validateToken(), admin::doAssignWork);
        app.get("/admin/projects", admin::projectTracking);
        app.get("/admin/projects/{id}", admin::projectDetails);
        app.get("/admin/material-requests", admin::materialRequests);
        app.post("/admin/material-requests/{id}/approve", csrfMiddleware.validateToken(), admin::approveMaterialRequest);
        app.post("/admin/material-requests/{id}/reject", csrfMiddleware.validateToken(), admin::rejectMaterialRequest);
        app.get("/admin/logs", admin::activityLogs);

        // Vendor routes
        app.get("/vendor/dashboard", vendor::dashboard);
        app.get("/vendor/projects", vendor::assignedWork);
        app.get("/vendor/projects/{id}", vendor::projectDetails);
        app.get("/vendor/daily-update", vendor::showDailyUpdate);
        app.post("/vendor/daily-update", csrfMiddleware.validateToken(), vendor::submitDailyUpdate);
        app.get("/vendor/material-request", vendor::showMaterialRequest);
        app.post("/vendor/material-request", csrfMiddleware.validateToken(), vendor::submitMaterialRequest);

        // API routes
        app.before("/api/*", authMiddleware.authenticate());
        
        // Notifications API
        app.get("/api/notifications", vendor::getNotifications);
        app.post("/api/notifications/{id}/read", vendor::markNotificationRead);
        app.post("/api/notifications/read-all", vendor::markAllNotificationsRead);

        // Clear auth context after each request
        app.after(authMiddleware.clearContext());
    }

    private static void setupErrorHandlers(Javalin app) {
        app.error(404, ctx -> {
            if (ctx.header("Accept") != null && ctx.header("Accept").contains("application/json")) {
                ctx.json(new ErrorResponse("Not found", 404));
            } else {
                ctx.render("error/404.html");
            }
        });

        app.error(500, ctx -> {
            logger.error("Internal server error", ctx.attribute("javalin-error"));
            if (ctx.header("Accept") != null && ctx.header("Accept").contains("application/json")) {
                ctx.json(new ErrorResponse("Internal server error", 500));
            } else {
                ctx.render("error/500.html");
            }
        });

        app.exception(Exception.class, (e, ctx) -> {
            logger.error("Unhandled exception", e);
            if (ctx.header("Accept") != null && ctx.header("Accept").contains("application/json")) {
                ctx.status(500).json(new ErrorResponse("An error occurred", 500));
            } else {
                ctx.status(500).render("error/500.html");
            }
        });
    }

    public static class HealthResponse {
        public final String status;
        public final String version;
        public final long timestamp;

        public HealthResponse(String status, String version) {
            this.status = status;
            this.version = version;
            this.timestamp = System.currentTimeMillis();
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
