package com.gtplug.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Project {
    private Long id;
    private String projectCode;
    private Long vendorId;
    private Long assignedByAdminId;
    private String startLocation;
    private String endLocation;
    private BigDecimal totalKm;
    private BigDecimal completedKm;
    private BigDecimal remainingKm;
    private BigDecimal costPerKm;
    private BigDecimal totalCost;
    private String workDescription;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate deadline;
    private LocalDateTime completedAt;
    private int progressPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient fields for joining
    private Vendor vendor;
    private Admin assignedByAdmin;

    public enum ProjectStatus {
        ASSIGNED, IN_PROGRESS, DELAYED, COMPLETED
    }

    public Project() {}

    public Project(String projectCode, Long vendorId, Long assignedByAdminId, 
                   String startLocation, String endLocation, BigDecimal totalKm,
                   BigDecimal costPerKm, String workDescription, LocalDate startDate, LocalDate deadline) {
        this.projectCode = projectCode;
        this.vendorId = vendorId;
        this.assignedByAdminId = assignedByAdminId;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.totalKm = totalKm;
        this.completedKm = BigDecimal.ZERO;
        this.remainingKm = totalKm;
        this.costPerKm = costPerKm;
        this.totalCost = totalKm.multiply(costPerKm);
        this.workDescription = workDescription;
        this.status = ProjectStatus.ASSIGNED;
        this.startDate = startDate;
        this.deadline = deadline;
        this.progressPercentage = 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public Long getAssignedByAdminId() { return assignedByAdminId; }
    public void setAssignedByAdminId(Long assignedByAdminId) { this.assignedByAdminId = assignedByAdminId; }

    public String getStartLocation() { return startLocation; }
    public void setStartLocation(String startLocation) { this.startLocation = startLocation; }

    public String getEndLocation() { return endLocation; }
    public void setEndLocation(String endLocation) { this.endLocation = endLocation; }

    public BigDecimal getTotalKm() { return totalKm; }
    public void setTotalKm(BigDecimal totalKm) { this.totalKm = totalKm; }

    public BigDecimal getCompletedKm() { return completedKm; }
    public void setCompletedKm(BigDecimal completedKm) { this.completedKm = completedKm; }

    public BigDecimal getRemainingKm() { return remainingKm; }
    public void setRemainingKm(BigDecimal remainingKm) { this.remainingKm = remainingKm; }

    public BigDecimal getCostPerKm() { return costPerKm; }
    public void setCostPerKm(BigDecimal costPerKm) { this.costPerKm = costPerKm; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public String getWorkDescription() { return workDescription; }
    public void setWorkDescription(String workDescription) { this.workDescription = workDescription; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public int getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }

    public Admin getAssignedByAdmin() { return assignedByAdmin; }
    public void setAssignedByAdmin(Admin assignedByAdmin) { this.assignedByAdmin = assignedByAdmin; }

    public boolean isOverdue() {
        return deadline.isBefore(LocalDate.now()) && status != ProjectStatus.COMPLETED;
    }

    public void updateStatus() {
        if (completedKm.compareTo(totalKm) >= 0) {
            status = ProjectStatus.COMPLETED;
        } else if (completedKm.compareTo(BigDecimal.ZERO) > 0) {
            status = isOverdue() ? ProjectStatus.DELAYED : ProjectStatus.IN_PROGRESS;
        }
        
        if (totalKm.compareTo(BigDecimal.ZERO) > 0) {
            progressPercentage = completedKm.multiply(new BigDecimal("100")).divide(totalKm, 0, BigDecimal.ROUND_HALF_UP).intValue();
        }
    }
}
