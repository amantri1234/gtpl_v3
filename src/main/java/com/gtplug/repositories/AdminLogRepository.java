package com.gtplug.repositories;

import com.gtplug.config.DatabaseConfig;
import com.gtplug.models.AdminLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminLogRepository {
    private static final Logger logger = LoggerFactory.getLogger(AdminLogRepository.class);
    private final DataSource dataSource;

    public AdminLogRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public AdminLog save(AdminLog log) {
        String sql = "INSERT INTO admin_logs (admin_id, action, project_id, vendor_id, details, ip_address, user_agent) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, log.getAdminId());
            stmt.setString(2, log.getAction());
            stmt.setObject(3, log.getProjectId(), Types.BIGINT);
            stmt.setObject(4, log.getVendorId(), Types.BIGINT);
            stmt.setString(5, log.getDetails());
            stmt.setString(6, log.getIpAddress());
            stmt.setString(7, log.getUserAgent());
            
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                log.setId(rs.getLong(1));
            }
            logger.debug("Admin log created: {}", log.getAction());
        } catch (SQLException e) {
            logger.error("Error creating admin log: {}", e.getMessage());
        }
        return log;
    }

    public List<AdminLog> findAll(int page, int size) {
        List<AdminLog> logs = new ArrayList<>();
        String sql = "SELECT al.*, a.full_name as admin_name, p.project_code, v.vendor_name " +
                     "FROM admin_logs al " +
                     "JOIN admins a ON al.admin_id = a.id " +
                     "LEFT JOIN projects p ON al.project_id = p.id " +
                     "LEFT JOIN vendors v ON al.vendor_id = v.id " +
                     "ORDER BY al.created_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, size);
            stmt.setInt(2, (page - 1) * size);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToAdminLog(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding admin logs: {}", e.getMessage());
        }
        return logs;
    }

    public List<AdminLog> findByAdminId(Long adminId, int page, int size) {
        List<AdminLog> logs = new ArrayList<>();
        String sql = "SELECT al.*, a.full_name as admin_name, p.project_code, v.vendor_name " +
                     "FROM admin_logs al " +
                     "JOIN admins a ON al.admin_id = a.id " +
                     "LEFT JOIN projects p ON al.project_id = p.id " +
                     "LEFT JOIN vendors v ON al.vendor_id = v.id " +
                     "WHERE al.admin_id = ? ORDER BY al.created_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, adminId);
            stmt.setInt(2, size);
            stmt.setInt(3, (page - 1) * size);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToAdminLog(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding admin logs by admin: {}", e.getMessage());
        }
        return logs;
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM admin_logs";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting admin logs: {}", e.getMessage());
        }
        return 0;
    }

    private AdminLog mapResultSetToAdminLog(ResultSet rs) throws SQLException {
        AdminLog log = new AdminLog();
        log.setId(rs.getLong("id"));
        log.setAdminId(rs.getLong("admin_id"));
        log.setAction(rs.getString("action"));
        
        long projectId = rs.getLong("project_id");
        if (!rs.wasNull()) {
            log.setProjectId(projectId);
        }
        
        long vendorId = rs.getLong("vendor_id");
        if (!rs.wasNull()) {
            log.setVendorId(vendorId);
        }
        
        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setUserAgent(rs.getString("user_agent"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            log.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        // Set transient fields
        var admin = new com.gtplug.models.Admin();
        admin.setId(log.getAdminId());
        admin.setFullName(rs.getString("admin_name"));
        log.setAdmin(admin);
        
        String projectCode = rs.getString("project_code");
        if (projectCode != null) {
            var project = new com.gtplug.models.Project();
            project.setId(log.getProjectId());
            project.setProjectCode(projectCode);
            log.setProject(project);
        }
        
        String vendorName = rs.getString("vendor_name");
        if (vendorName != null) {
            var vendor = new com.gtplug.models.Vendor();
            vendor.setId(log.getVendorId());
            vendor.setVendorName(vendorName);
            log.setVendor(vendor);
        }
        
        return log;
    }
}
