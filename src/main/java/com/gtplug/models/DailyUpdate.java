package com.gtplug.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DailyUpdate {
    private Long id;
    private Long projectId;
    private Long vendorId;
    private LocalDate updateDate;
    private BigDecimal kmCompleted;
    private String workDescription;
    private String photoProofPath;
    private BigDecimal cumulativeKm;
    private LocalDateTime createdAt;

    // Transient fields
    private Project project;
    private Vendor vendor;

    public DailyUpdate() {}

    public DailyUpdate(Long projectId, Long vendorId, LocalDate updateDate, 
                       BigDecimal kmCompleted, String workDescription, BigDecimal cumulativeKm) {
        this.projectId = projectId;
        this.vendorId = vendorId;
        this.updateDate = updateDate;
        this.kmCompleted = kmCompleted;
        this.workDescription = workDescription;
        this.cumulativeKm = cumulativeKm;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public LocalDate getUpdateDate() { return updateDate; }
    public void setUpdateDate(LocalDate updateDate) { this.updateDate = updateDate; }

    public BigDecimal getKmCompleted() { return kmCompleted; }
    public void setKmCompleted(BigDecimal kmCompleted) { this.kmCompleted = kmCompleted; }

    public String getWorkDescription() { return workDescription; }
    public void setWorkDescription(String workDescription) { this.workDescription = workDescription; }

    public String getPhotoProofPath() { return photoProofPath; }
    public void setPhotoProofPath(String photoProofPath) { this.photoProofPath = photoProofPath; }

    public BigDecimal getCumulativeKm() { return cumulativeKm; }
    public void setCumulativeKm(BigDecimal cumulativeKm) { this.cumulativeKm = cumulativeKm; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
}
