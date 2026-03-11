package com.gtplug.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Vendor {
    private Long id;
    private Long userId;
    private String vendorName;
    private String mobileNumber;
    private LocalDate registrationDate;
    private int totalProjects;
    private int activeProjects;
    private int completedProjects;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient field for joining with User
    private User user;

    public Vendor() {}

    public Vendor(Long userId, String vendorName, String mobileNumber, LocalDate registrationDate) {
        this.userId = userId;
        this.vendorName = vendorName;
        this.mobileNumber = mobileNumber;
        this.registrationDate = registrationDate;
        this.totalProjects = 0;
        this.activeProjects = 0;
        this.completedProjects = 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public LocalDate getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDate registrationDate) { this.registrationDate = registrationDate; }

    public int getTotalProjects() { return totalProjects; }
    public void setTotalProjects(int totalProjects) { this.totalProjects = totalProjects; }

    public int getActiveProjects() { return activeProjects; }
    public void setActiveProjects(int activeProjects) { this.activeProjects = activeProjects; }

    public int getCompletedProjects() { return completedProjects; }
    public void setCompletedProjects(int completedProjects) { this.completedProjects = completedProjects; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
