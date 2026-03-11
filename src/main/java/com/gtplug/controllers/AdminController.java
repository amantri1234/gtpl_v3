package com.gtplug.controllers;

import com.gtplug.models.*;
import com.gtplug.security.AuthContext;
import com.gtplug.services.*;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService;
    private final VendorService vendorService;
    private final ProjectService projectService;
    private final MaterialRequestService materialRequestService;
    private final AdminLogService adminLogService;
    private final NotificationService notificationService;

    public AdminController() {
        this.adminService = new AdminService();
        this.vendorService = new VendorService();
        this.projectService = new ProjectService();
        this.materialRequestService = new MaterialRequestService();
        this.adminLogService = new AdminLogService();
        this.notificationService = new NotificationService();
    }

    public void dashboard(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        User currentUser = ctx.attribute("currentUser");
        
        // Get stats
        long totalVendors = vendorService.count("");
        long totalProjects = projectService.count("", "");
        long pendingRequests = materialRequestService.count("PENDING");
        long unreadNotifications = notificationService.getUnreadCount(currentUser.getId());
        
        model.put("totalVendors", totalVendors);
        model.put("totalProjects", totalProjects);
        model.put("pendingRequests", pendingRequests);
        model.put("unreadNotifications", unreadNotifications);
        model.put("currentUser", currentUser);
        
        ctx.render("admin/dashboard.html", model);
    }

    public void vendorCatalog(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        
        int page = parseInt(ctx.queryParam("page"), 1);
        String search = ctx.queryParam("search");
        
        List<Vendor> vendors = vendorService.findAll(page, 10, search);
        long totalVendors = vendorService.count(search);
        int totalPages = (int) Math.ceil((double) totalVendors / 10);
        
        model.put("vendors", vendors);
        model.put("currentPage", page);
        model.put("totalPages", totalPages);
        model.put("search", search);
        model.put("currentUser", ctx.attribute("currentUser"));
        
        ctx.render("admin/vendors.html", model);
    }

    public void searchVendors(Context ctx) {
        String search = ctx.queryParam("q");
        List<Vendor> vendors = vendorService.findAll(1, 50, search);
        ctx.json(vendors);
    }

    public void showAssignWork(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        
        List<Vendor> vendors = vendorService.findAll(1, 100, null);
        model.put("vendors", vendors);
        model.put("currentUser", ctx.attribute("currentUser"));
        model.put("csrfToken", ctx.attribute("csrfToken"));
        
        String error = ctx.queryParam("error");
        if (error != null) {
            model.put("error", error);
        }
        
        String success = ctx.queryParam("success");
        if (success != null) {
            model.put("success", "Project assigned successfully!");
        }
        
        ctx.render("admin/assign-work.html", model);
    }

    public void doAssignWork(Context ctx) {
        try {
            Long vendorId = Long.parseLong(ctx.formParam("vendorId"));
            String startLocation = ctx.formParam("startLocation");
            String endLocation = ctx.formParam("endLocation");
            BigDecimal totalKm = new BigDecimal(ctx.formParam("totalKm"));
            BigDecimal costPerKm = new BigDecimal(ctx.formParam("costPerKm"));
            String workDescription = ctx.formParam("workDescription");
            LocalDate startDate = LocalDate.parse(ctx.formParam("startDate"));
            LocalDate deadline = LocalDate.parse(ctx.formParam("deadline"));
            String adminName = ctx.formParam("adminName");
            
            User currentUser = ctx.attribute("currentUser");
            Admin admin = adminService.findByUserId(currentUser.getId()).orElseThrow();
            
            var result = projectService.assignProject(
                vendorId, admin.getId(), startLocation, endLocation, 
                totalKm, costPerKm, workDescription, startDate, deadline,
                adminName, ctx.ip(), ctx.userAgent()
            );
            
            if (!result.isSuccess()) {
                ctx.redirect("/admin/assign-work?error=" + result.getMessage().replace(" ", "+"));
                return;
            }
            
            ctx.redirect("/admin/assign-work?success=true");
        } catch (Exception e) {
            logger.error("Error assigning work: {}", e.getMessage());
            ctx.redirect("/admin/assign-work?error=Invalid+input+data");
        }
    }

    public void projectTracking(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        
        int page = parseInt(ctx.queryParam("page"), 1);
        String status = ctx.queryParam("status");
        String search = ctx.queryParam("search");
        
        List<Project> projects = projectService.findAll(page, 10, status, search);
        long totalProjects = projectService.count(status, search);
        int totalPages = (int) Math.ceil((double) totalProjects / 10);
        
        model.put("projects", projects);
        model.put("currentPage", page);
        model.put("totalPages", totalPages);
        model.put("status", status);
        model.put("search", search);
        model.put("currentUser", ctx.attribute("currentUser"));
        
        ctx.render("admin/projects.html", model);
    }

    public void projectDetails(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        
        try {
            Long projectId = Long.parseLong(ctx.pathParam("id"));
            Project project = projectService.findById(projectId).orElse(null);
            
            if (project == null) {
                ctx.status(404).render("error/404.html");
                return;
            }
            
            DailyUpdateService dailyUpdateService = new DailyUpdateService();
            List<DailyUpdate> updates = dailyUpdateService.findByProjectId(projectId, 1, 50);
            
            model.put("project", project);
            model.put("updates", updates);
            model.put("currentUser", ctx.attribute("currentUser"));
            
            ctx.render("admin/project-details.html", model);
        } catch (NumberFormatException e) {
            ctx.status(404).render("error/404.html");
        }
    }

    public void materialRequests(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        
        int page = parseInt(ctx.queryParam("page"), 1);
        String status = ctx.queryParam("status");
        
        List<MaterialRequest> requests = materialRequestService.findAll(status, page, 10);
        long totalRequests = materialRequestService.count(status);
        int totalPages = (int) Math.ceil((double) totalRequests / 10);
        
        model.put("requests", requests);
        model.put("currentPage", page);
        model.put("totalPages", totalPages);
        model.put("status", status);
        model.put("currentUser", ctx.attribute("currentUser"));
        model.put("csrfToken", ctx.attribute("csrfToken"));
        
        ctx.render("admin/material-requests.html", model);
    }

    public void approveMaterialRequest(Context ctx) {
        try {
            Long requestId = Long.parseLong(ctx.pathParam("id"));
            String remarks = ctx.formParam("remarks");
            
            User currentUser = ctx.attribute("currentUser");
            Admin admin = adminService.findByUserId(currentUser.getId()).orElseThrow();
            
            var result = materialRequestService.approveRequest(
                requestId, admin.getId(), remarks, ctx.ip(), ctx.userAgent()
            );
            
            if (!result.isSuccess()) {
                ctx.redirect("/admin/material-requests?error=" + result.getMessage().replace(" ", "+"));
                return;
            }
            
            ctx.redirect("/admin/material-requests?success=approved");
        } catch (Exception e) {
            logger.error("Error approving request: {}", e.getMessage());
            ctx.redirect("/admin/material-requests?error=Failed+to+approve+request");
        }
    }

    public void rejectMaterialRequest(Context ctx) {
        try {
            Long requestId = Long.parseLong(ctx.pathParam("id"));
            String remarks = ctx.formParam("remarks");
            
            if (remarks == null || remarks.isEmpty()) {
                ctx.redirect("/admin/material-requests?error=Remarks+are+required+for+rejection");
                return;
            }
            
            User currentUser = ctx.attribute("currentUser");
            Admin admin = adminService.findByUserId(currentUser.getId()).orElseThrow();
            
            var result = materialRequestService.rejectRequest(
                requestId, admin.getId(), remarks, ctx.ip(), ctx.userAgent()
            );
            
            if (!result.isSuccess()) {
                ctx.redirect("/admin/material-requests?error=" + result.getMessage().replace(" ", "+"));
                return;
            }
            
            ctx.redirect("/admin/material-requests?success=rejected");
        } catch (Exception e) {
            logger.error("Error rejecting request: {}", e.getMessage());
            ctx.redirect("/admin/material-requests?error=Failed+to+reject+request");
        }
    }

    public void activityLogs(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        
        int page = parseInt(ctx.queryParam("page"), 1);
        
        List<AdminLog> logs = adminLogService.getAllLogs(page, 20);
        long totalLogs = adminLogService.count();
        int totalPages = (int) Math.ceil((double) totalLogs / 20);
        
        model.put("logs", logs);
        model.put("currentPage", page);
        model.put("totalPages", totalPages);
        model.put("currentUser", ctx.attribute("currentUser"));
        
        ctx.render("admin/logs.html", model);
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
