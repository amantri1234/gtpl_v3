package com.gtplug.repositories;

import com.gtplug.config.DatabaseConfig;
import com.gtplug.models.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotificationRepository {
    private static final Logger logger = LoggerFactory.getLogger(NotificationRepository.class);
    private final DataSource dataSource;

    public NotificationRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public Optional<Notification> findById(Long id) {
        String sql = "SELECT n.*, p.project_code FROM notifications n " +
                     "LEFT JOIN projects p ON n.related_project_id = p.id WHERE n.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToNotification(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding notification by ID: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Notification save(Notification notification) {
        String sql = "INSERT INTO notifications (user_id, title, message, type, related_project_id) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, notification.getUserId());
            stmt.setString(2, notification.getTitle());
            stmt.setString(3, notification.getMessage());
            stmt.setString(4, notification.getType().name());
            stmt.setObject(5, notification.getRelatedProjectId(), Types.BIGINT);
            
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                notification.setId(rs.getLong(1));
            }
            logger.debug("Notification created for user: {}", notification.getUserId());
        } catch (SQLException e) {
            logger.error("Error creating notification: {}", e.getMessage());
        }
        return notification;
    }

    public List<Notification> findByUserId(Long userId, boolean unreadOnly, int page, int size) {
        List<Notification> notifications = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT n.*, p.project_code FROM notifications n " +
            "LEFT JOIN projects p ON n.related_project_id = p.id " +
            "WHERE n.user_id = ? "
        );
        
        if (unreadOnly) {
            sql.append("AND n.is_read = FALSE ");
        }
        
        sql.append("ORDER BY n.created_at DESC LIMIT ? OFFSET ?");
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setLong(1, userId);
            stmt.setInt(2, size);
            stmt.setInt(3, (page - 1) * size);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding notifications by user: {}", e.getMessage());
        }
        return notifications;
    }

    public long countUnreadByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting unread notifications: {}", e.getMessage());
        }
        return 0;
    }

    public void markAsRead(Long notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, notificationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error marking notification as read: {}", e.getMessage());
        }
    }

    public void markAllAsRead(Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error marking all notifications as read: {}", e.getMessage());
        }
    }

    public void deleteOldNotifications(int days) {
        String sql = "DELETE FROM notifications WHERE created_at < DATE_SUB(NOW(), INTERVAL ? DAY)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, days);
            int deleted = stmt.executeUpdate();
            logger.info("Deleted {} old notifications", deleted);
        } catch (SQLException e) {
            logger.error("Error deleting old notifications: {}", e.getMessage());
        }
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getLong("id"));
        notification.setUserId(rs.getLong("user_id"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setType(Notification.NotificationType.valueOf(rs.getString("type")));
        notification.setRead(rs.getBoolean("is_read"));
        
        long projectId = rs.getLong("related_project_id");
        if (!rs.wasNull()) {
            notification.setRelatedProjectId(projectId);
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        // Set transient fields
        String projectCode = rs.getString("project_code");
        if (projectCode != null) {
            var project = new com.gtplug.models.Project();
            project.setId(notification.getRelatedProjectId());
            project.setProjectCode(projectCode);
            notification.setRelatedProject(project);
        }
        
        return notification;
    }
}
