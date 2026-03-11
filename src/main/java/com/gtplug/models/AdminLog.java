package com.gtplug.models;

import java.time.LocalDateTime;

public class AdminLog {
    private Long id;
    private Long adminId;
    private String action;
    private Long projectId;
    private Long vendorId;
    private String details;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;

    // Transient fields
    private Admin admin;
    private Project project;
    private Vendor vendor;

    public AdminLog() {}

    public AdminLog(Long adminId, String action, String details) {
        this.adminId = adminId;
        this.action = action;
        this.details = details;
    }

    public AdminLog(Long adminId, String action, Long projectId, Long vendorId, String details) {
        this.adminId = adminId;
        this.action = action;
        this.projectId = projectId;
        this.vendorId = vendorId;
        this.details = details;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
}
