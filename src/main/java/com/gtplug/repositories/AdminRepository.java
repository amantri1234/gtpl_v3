package com.gtplug.repositories;

import com.gtplug.config.DatabaseConfig;
import com.gtplug.models.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminRepository {
    private static final Logger logger = LoggerFactory.getLogger(AdminRepository.class);
    private final DataSource dataSource;

    public AdminRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public Optional<Admin> findById(Long id) {
        String sql = "SELECT a.*, u.username, u.email FROM admins a " +
                     "JOIN users u ON a.user_id = u.id WHERE a.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToAdmin(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding admin by ID: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Admin> findByUserId(Long userId) {
        String sql = "SELECT a.*, u.username, u.email FROM admins a " +
                     "JOIN users u ON a.user_id = u.id WHERE a.user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToAdmin(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding admin by user ID: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Admin save(Admin admin) {
        if (admin.getId() == null) {
            return insert(admin);
        } else {
            return update(admin);
        }
    }

    private Admin insert(Admin admin) {
        String sql = "INSERT INTO admins (user_id, full_name, mobile_number, department) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, admin.getUserId());
            stmt.setString(2, admin.getFullName());
            stmt.setString(3, admin.getMobileNumber());
            stmt.setString(4, admin.getDepartment());
            
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                admin.setId(rs.getLong(1));
            }
            logger.info("Admin created: {}", admin.getFullName());
        } catch (SQLException e) {
            logger.error("Error creating admin: {}", e.getMessage());
            throw new RuntimeException("Failed to create admin", e);
        }
        return admin;
    }

    private Admin update(Admin admin) {
        String sql = "UPDATE admins SET full_name = ?, mobile_number = ?, department = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, admin.getFullName());
            stmt.setString(2, admin.getMobileNumber());
            stmt.setString(3, admin.getDepartment());
            stmt.setLong(4, admin.getId());
            stmt.executeUpdate();
            logger.debug("Admin updated: {}", admin.getFullName());
        } catch (SQLException e) {
            logger.error("Error updating admin: {}", e.getMessage());
            throw new RuntimeException("Failed to update admin", e);
        }
        return admin;
    }

    public List<Admin> findAll() {
        List<Admin> admins = new ArrayList<>();
        String sql = "SELECT a.*, u.username, u.email FROM admins a " +
                     "JOIN users u ON a.user_id = u.id ORDER BY a.created_at DESC";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                admins.add(mapResultSetToAdmin(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all admins: {}", e.getMessage());
        }
        return admins;
    }

    public void delete(Long id) {
        String sql = "DELETE FROM admins WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
            logger.info("Admin deleted: {}", id);
        } catch (SQLException e) {
            logger.error("Error deleting admin: {}", e.getMessage());
            throw new RuntimeException("Failed to delete admin", e);
        }
    }

    private Admin mapResultSetToAdmin(ResultSet rs) throws SQLException {
        Admin admin = new Admin();
        admin.setId(rs.getLong("id"));
        admin.setUserId(rs.getLong("user_id"));
        admin.setFullName(rs.getString("full_name"));
        admin.setMobileNumber(rs.getString("mobile_number"));
        admin.setDepartment(rs.getString("department"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            admin.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            admin.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        // Map joined user fields
        var user = new com.gtplug.models.User();
        user.setId(admin.getUserId());
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        admin.setUser(user);
        
        return admin;
    }
}
