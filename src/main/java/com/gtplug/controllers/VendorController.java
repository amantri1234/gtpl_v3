package com.gtplug.controllers;

import com.gtplug.models.*;
import com.gtplug.services.*;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VendorController {
    private static final Logger logger = LoggerFactory.getLogger(VendorController.class);
    private final VendorService vendorService;
    private final ProjectService projectService;
    private final DailyUpdateService dailyUpdateService;
    private final MaterialRequestService materialRequestService;
    private final NotificationService notificationService;

    public VendorController() {
        this.vendorService = new VendorService();
        this.projectService = new ProjectService();
        this.dailyUpdateService = new DailyUpdateService();
        this.materialRequestService = new MaterialRequestService();
        this.notificationService = new NotificationService();
    }

    public void dashboard(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        User currentUser = ctx.attribute("currentUser");
        
        Vendor vendor = vendorService.findByUserId(currentUser.getId()).orElse(null);
        if (vendor == null) {
            ctx.status(500).render("error/500.html");
            return;
        }
        
        // Get vendor's projects
        List<Project> projects = projectService.findByVendorId(vendor.getId(), 1, 5);
        long unreadNotifications = notificationService.getUnreadCount(currentUser.getId());
        
        // Check for pending daily updates
        boolean hasPendingUpdate = false;
        for (Project project : projects) {
            if (project.getStatus() != Project.ProjectStatus.COMPLETED && 
                !dailyUpdateService.hasUpdateForToday(project.getId())) {
                hasPendingUpdate = true;
                break;
            }
        }
        
        model.put("vendor", vendor);
        model.put("projects", projects);
        model.put("unreadNotifications", unreadNotifications);
        model.put("hasPendingUpdate", hasPendingUpdate);
        model.put("currentUser", currentUser);
        
        ctx.render("vendor/dashboard.html", model);
    }

    public void assignedWork(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        User currentUser = ctx.attribute("currentUser");
        
        Vendor vendor = vendorService.findByUserId(currentUser.getId()).orElse(null);
        if (vendor == null) {
            ctx.status(500).render("error/500.html");
            return;
        }
        
        int page = parseInt(ctx.queryParam("page"), 1);
        List<Project> projects = projectService.findByVendorId(vendor.getId(), page, 10);
        
        model.put("vendor", vendor);
        model.put("projects", projects);
        model.put("currentPage", page);
        model.put("currentUser", currentUser);
        
        ctx.render("vendor/projects.html", model);
    }

    public void projectDetails(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        User currentUser = ctx.attribute("currentUser");
        
        try {
            Long projectId = Long.parseLong(ctx.pathParam("id"));
            
            Vendor vendor = vendorService.findByUserId(currentUser.getId()).orElse(null);
            if (vendor == null) {
                ctx.status(500).render("error/500.html");
                return;
            }
            
            Project project = projectService.findById(projectId).orElse(null);
            if (project == null || !project.getVendorId().equals(vendor.getId())) {
                ctx.status(404).render("error/404.html");
                return;
            }
            
            List<DailyUpdate> updates = dailyUpdateService.findByProjectId(projectId, 1, 50);
            List<MaterialRequest> materialRequests = materialRequestService.findByProjectId(projectId);
            
            model.put("project", project);
            model.put("updates", updates);
            model.put("materialRequests", materialRequests);
            model.put("vendor", vendor);
            model.put("currentUser", currentUser);
            
            ctx.render("vendor/project-details.html", model);
        } catch (NumberFormatException e) {
            ctx.status(404).render("error/404.html");
        }
    }

    public void showDailyUpdate(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        User currentUser = ctx.attribute("currentUser");
        
        Vendor vendor = vendorService.findByUserId(currentUser.getId()).orElse(null);
        if (vendor == null) {
            ctx.status(500).render("error/500.html");
            return;
        }
        
        List<Project> activeProjects = projectService.findByVendorId(vendor.getId(), 1, 100);
        // Filter only non-completed projects
        activeProjects.removeIf(p -> p.getStatus() == Project.ProjectStatus.COMPLETED);
        
        String error = ctx.queryParam("error");
        if (error != null) {
            model.put("error", error);
        }
        
        String success = ctx.queryParam("success");
        if (success != null) {
            model.put("success", "Daily update submitted successfully!");
        }
        
        model.put("projects", activeProjects);
        model.put("today", LocalDate.now());
        model.put("vendor", vendor);
        model.put("currentUser", currentUser);
        model.put("csrfToken", ctx.attribute("csrfToken"));
        
        ctx.render("vendor/daily-update.html", model);
    }

    public void submitDailyUpdate(Context ctx) {
        try {
            Long projectId = Long.parseLong(ctx.formParam("projectId"));
            LocalDate updateDate = LocalDate.parse(ctx.formParam("updateDate"));
            BigDecimal kmCompleted = new BigDecimal(ctx.formParam("kmCompleted"));
            String workDescription = ctx.formParam("workDescription");
            
            User currentUser = ctx.attribute("currentUser");
            Vendor vendor = vendorService.findByUserId(currentUser.getId()).orElseThrow();
            
            // Handle photo upload
            byte[] photoData = null;
            String photoFilename = null;
            UploadedFile uploadedFile = ctx.uploadedFile("photoProof");
            if (uploadedFile != null) {
                photoData = uploadedFile.content().readAllBytes();
                photoFilename = uploadedFile.filename();
            }
            
            var result = dailyUpdateService.submitUpdate(
                projectId, vendor.getId(), updateDate, 
                kmCompleted, workDescription, photoData, photoFilename
            );
            
            if (!result.isSuccess()) {
                ctx.redirect("/vendor/daily-update?error=" + result.getMessage().replace(" ", "+"));
                return;
            }
            
            ctx.redirect("/vendor/daily-update?success=true");
        } catch (Exception e) {
            logger.error("Error submitting daily update: {}", e.getMessage());
            ctx.redirect("/vendor/daily-update?error=Invalid+input+data");
        }
    }

    public void showMaterialRequest(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        User currentUser = ctx.attribute("currentUser");
        
        Vendor vendor = vendorService.findByUserId(currentUser.getId()).orElse(null);
        if (vendor == null) {
            ctx.status(500).render("error/500.html");
            return;
        }
        
        List<Project> activeProjects = projectService.findByVendorId(vendor.getId(), 1, 100);
        activeProjects.removeIf(p -> p.getStatus() == Project.ProjectStatus.COMPLETED);
        
        String error = ctx.queryParam("error");
        if (error != null) {
            model.put("error", error);
        }
        
        String success = ctx.queryParam("success");
        if (success != null) {
            model.put("success", "Material request submitted successfully!");
        }
        
        model.put("projects", activeProjects);
        model.put("vendor", vendor);
        model.put("currentUser", currentUser);
        model.put("csrfToken", ctx.attribute("csrfToken"));
        
        ctx.render("vendor/material-request.html", model);
    }

    public void submitMaterialRequest(Context ctx) {
        try {
            Long projectId = Long.parseLong(ctx.formParam("projectId"));
            String cableKmStr = ctx.formParam("additionalCableKm");
            String ductKmStr = ctx.formParam("additionalDuctKm");
            String reason = ctx.formParam("reason");
            
            BigDecimal cableKm = (cableKmStr != null && !cableKmStr.isEmpty()) ? 
                new BigDecimal(cableKmStr) : BigDecimal.ZERO;
            BigDecimal ductKm = (ductKmStr != null && !ductKmStr.isEmpty()) ? 
                new BigDecimal(ductKmStr) : BigDecimal.ZERO;
            
            User currentUser = ctx.attribute("currentUser");
            Vendor vendor = vendorService.findByUserId(currentUser.getId()).orElseThrow();
            
            var result = materialRequestService.createRequest(
                projectId, vendor.getId(), cableKm, ductKm, reason
            );
            
            if (!result.isSuccess()) {
                ctx.redirect("/vendor/material-request?error=" + result.getMessage().replace(" ", "+"));
                return;
            }
            
            ctx.redirect("/vendor/material-request?success=true");
        } catch (Exception e) {
            logger.error("Error submitting material request: {}", e.getMessage());
            ctx.redirect("/vendor/material-request?error=Invalid+input+data");
        }
    }

    public void getNotifications(Context ctx) {
        User currentUser = ctx.attribute("currentUser");
        boolean unreadOnly = "true".equals(ctx.queryParam("unreadOnly"));
        int page = parseInt(ctx.queryParam("page"), 1);
        
        List<Notification> notifications = notificationService.getUserNotifications(
            currentUser.getId(), unreadOnly, page, 20
        );
        long unreadCount = notificationService.getUnreadCount(currentUser.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notifications);
        response.put("unreadCount", unreadCount);
        
        ctx.json(response);
    }

    public void markNotificationRead(Context ctx) {
        try {
            Long notificationId = Long.parseLong(ctx.pathParam("id"));
            notificationService.markAsRead(notificationId);
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "Invalid notification ID"));
        }
    }

    public void markAllNotificationsRead(Context ctx) {
        User currentUser = ctx.attribute("currentUser");
        notificationService.markAllAsRead(currentUser.getId());
        ctx.json(Map.of("success", true));
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
