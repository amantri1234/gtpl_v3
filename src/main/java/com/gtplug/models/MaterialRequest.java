package com.gtplug.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MaterialRequest {
    private Long id;
    private Long projectId;
    private Long vendorId;
    private BigDecimal additionalCableKm;
    private BigDecimal additionalDuctKm;
    private String reason;
    private RequestStatus status;
    private LocalDateTime requestedAt;
    private Long approvedByAdminId;
    private LocalDateTime approvedAt;
    private String adminRemarks;

    // Transient fields
    private Project project;
    private Vendor vendor;
    private Admin approvedByAdmin;

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }

    public MaterialRequest() {}

    public MaterialRequest(Long projectId, Long vendorId, BigDecimal additionalCableKm, 
                           BigDecimal additionalDuctKm, String reason) {
        this.projectId = projectId;
        this.vendorId = vendorId;
        this.additionalCableKm = additionalCableKm != null ? additionalCableKm : BigDecimal.ZERO;
        this.additionalDuctKm = additionalDuctKm != null ? additionalDuctKm : BigDecimal.ZERO;
        this.reason = reason;
        this.status = RequestStatus.PENDING;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public BigDecimal getAdditionalCableKm() { return additionalCableKm; }
    public void setAdditionalCableKm(BigDecimal additionalCableKm) { this.additionalCableKm = additionalCableKm; }

    public BigDecimal getAdditionalDuctKm() { return additionalDuctKm; }
    public void setAdditionalDuctKm(BigDecimal additionalDuctKm) { this.additionalDuctKm = additionalDuctKm; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public Long getApprovedByAdminId() { return approvedByAdminId; }
    public void setApprovedByAdminId(Long approvedByAdminId) { this.approvedByAdminId = approvedByAdminId; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getAdminRemarks() { return adminRemarks; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }

    public Admin getApprovedByAdmin() { return approvedByAdmin; }
    public void setApprovedByAdmin(Admin approvedByAdmin) { this.approvedByAdmin = approvedByAdmin; }

    public void approve(Long adminId, String remarks) {
        this.status = RequestStatus.APPROVED;
        this.approvedByAdminId = adminId;
        this.approvedAt = LocalDateTime.now();
        this.adminRemarks = remarks;
    }

    public void reject(Long adminId, String remarks) {
        this.status = RequestStatus.REJECTED;
        this.approvedByAdminId = adminId;
        this.approvedAt = LocalDateTime.now();
        this.adminRemarks = remarks;
    }
}
