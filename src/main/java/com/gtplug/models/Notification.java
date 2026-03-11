package com.gtplug.models;

import java.time.LocalDateTime;

public class Notification {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private NotificationType type;
    private boolean isRead;
    private Long relatedProjectId;
    private LocalDateTime createdAt;

    // Transient fields
    private User user;
    private Project relatedProject;

    public enum NotificationType {
        INFO, WARNING, SUCCESS, ERROR
    }

    public Notification() {}

    public Notification(Long userId, String title, String message, NotificationType type) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = false;
    }

    public Notification(Long userId, String title, String message, NotificationType type, Long relatedProjectId) {
        this(userId, title, message, type);
        this.relatedProjectId = relatedProjectId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Long getRelatedProjectId() { return relatedProjectId; }
    public void setRelatedProjectId(Long relatedProjectId) { this.relatedProjectId = relatedProjectId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Project getRelatedProject() { return relatedProject; }
    public void setRelatedProject(Project relatedProject) { this.relatedProject = relatedProject; }

    public void markAsRead() {
        this.isRead = true;
    }

    public String getTypeClass() {
        return switch (type) {
            case INFO -> "bg-blue-100 text-blue-800 border-blue-200";
            case WARNING -> "bg-yellow-100 text-yellow-800 border-yellow-200";
            case SUCCESS -> "bg-green-100 text-green-800 border-green-200";
            case ERROR -> "bg-red-100 text-red-800 border-red-200";
        };
    }

    public String getTypeIcon() {
        return switch (type) {
            case INFO -> "ℹ️";
            case WARNING -> "⚠️";
            case SUCCESS -> "✅";
            case ERROR -> "❌";
        };
    }
}
