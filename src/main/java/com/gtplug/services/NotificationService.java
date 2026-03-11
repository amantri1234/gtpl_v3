package com.gtplug.services;

import com.gtplug.models.Notification;
import com.gtplug.repositories.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notificationRepository;

    public NotificationService() {
        this.notificationRepository = new NotificationRepository();
    }

    public List<Notification> getUserNotifications(Long userId, boolean unreadOnly, int page, int size) {
        return notificationRepository.findByUserId(userId, unreadOnly, page, size);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    public Notification createNotification(Long userId, String title, String message, 
                                           Notification.NotificationType type) {
        Notification notification = new Notification(userId, title, message, type);
        return notificationRepository.save(notification);
    }

    public Notification createNotification(Long userId, String title, String message, 
                                           Notification.NotificationType type, Long projectId) {
        Notification notification = new Notification(userId, title, message, type, projectId);
        return notificationRepository.save(notification);
    }

    public void cleanupOldNotifications(int days) {
        notificationRepository.deleteOldNotifications(days);
    }
}
