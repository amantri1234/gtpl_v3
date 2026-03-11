package com.gtplug.repositories;

import com.gtplug.config.DatabaseConfig;
import com.gtplug.models.Vendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VendorRepository {
    private static final Logger logger = LoggerFactory.getLogger(VendorRepository.class);
    private final DataSource dataSource;

    public VendorRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public Optional<Vendor> findById(Long id) {
        String sql = "SELECT v.*, u.username, u.email, u.is_active FROM vendors v " +
                     "JOIN users u ON v.user_id = u.id WHERE v.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToVendor(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding vendor by ID: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Vendor> findByUserId(Long userId) {
        String sql = "SELECT v.*, u.username, u.email, u.is_active FROM vendors v " +
                     "JOIN users u ON v.user_id = u.id WHERE v.user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToVendor(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding vendor by user ID: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public boolean existsByMobileNumber(String mobileNumber) {
        String sql = "SELECT 1 FROM vendors WHERE mobile_number = ? LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, mobileNumber);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Error checking mobile number existence: {}", e.getMessage());
        }
        return false;
    }

    public Vendor save(Vendor vendor) {
        if (vendor.getId() == null) {
            return insert(vendor);
        } else {
            return update(vendor);
        }
    }

    private Vendor insert(Vendor vendor) {
        String sql = "INSERT INTO vendors (user_id, vendor_name, mobile_number, registration_date) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, vendor.getUserId());
            stmt.setString(2, vendor.getVendorName());
            stmt.setString(3, vendor.getMobileNumber());
            stmt.setDate(4, Date.valueOf(vendor.getRegistrationDate()));
            
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                vendor.setId(rs.getLong(1));
            }
            logger.info("Vendor created: {}", vendor.getVendorName());
        } catch (SQLException e) {
            logger.error("Error creating vendor: {}", e.getMessage());
            throw new RuntimeException("Failed to create vendor", e);
        }
        return vendor;
    }

    private Vendor update(Vendor vendor) {
        String sql = "UPDATE vendors SET vendor_name = ?, mobile_number = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vendor.getVendorName());
            stmt.setString(2, vendor.getMobileNumber());
            stmt.setLong(3, vendor.getId());
            stmt.executeUpdate();
            logger.debug("Vendor updated: {}", vendor.getVendorName());
        } catch (SQLException e) {
            logger.error("Error updating vendor: {}", e.getMessage());
            throw new RuntimeException("Failed to update vendor", e);
        }
        return vendor;
    }

    public List<Vendor> findAll(int page, int size, String search) {
        List<Vendor> vendors = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT v.*, u.username, u.email, u.is_active FROM vendors v " +
            "JOIN users u ON v.user_id = u.id "
        );
        
        if (search != null && !search.isEmpty()) {
            sql.append("WHERE v.vendor_name LIKE ? OR u.username LIKE ? OR u.email LIKE ? ");
        }
        
        sql.append("ORDER BY v.created_at DESC LIMIT ? OFFSET ?");
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search + "%";
                stmt.setString(paramIndex++, searchPattern);
                stmt.setString(paramIndex++, searchPattern);
                stmt.setString(paramIndex++, searchPattern);
            }
            
            stmt.setInt(paramIndex++, size);
            stmt.setInt(paramIndex, (page - 1) * size);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                vendors.add(mapResultSetToVendor(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all vendors: {}", e.getMessage());
        }
        return vendors;
    }

    public long count(String search) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM vendors v JOIN users u ON v.user_id = u.id "
        );
        
        if (search != null && !search.isEmpty()) {
            sql.append("WHERE v.vendor_name LIKE ? OR u.username LIKE ? OR u.email LIKE ?");
        }
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search + "%";
                stmt.setString(1, searchPattern);
                stmt.setString(2, searchPattern);
                stmt.setString(3, searchPattern);
            }
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting vendors: {}", e.getMessage());
        }
        return 0;
    }

    public void delete(Long id) {
        String sql = "DELETE FROM vendors WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
            logger.info("Vendor deleted: {}", id);
        } catch (SQLException e) {
            logger.error("Error deleting vendor: {}", e.getMessage());
            throw new RuntimeException("Failed to delete vendor", e);
        }
    }

    private Vendor mapResultSetToVendor(ResultSet rs) throws SQLException {
        Vendor vendor = new Vendor();
        vendor.setId(rs.getLong("id"));
        vendor.setUserId(rs.getLong("user_id"));
        vendor.setVendorName(rs.getString("vendor_name"));
        vendor.setMobileNumber(rs.getString("mobile_number"));
        
        Date regDate = rs.getDate("registration_date");
        if (regDate != null) {
            vendor.setRegistrationDate(regDate.toLocalDate());
        }
        
        vendor.setTotalProjects(rs.getInt("total_projects"));
        vendor.setActiveProjects(rs.getInt("active_projects"));
        vendor.setCompletedProjects(rs.getInt("completed_projects"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            vendor.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            vendor.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        // Map joined user fields
        var user = new com.gtplug.models.User();
        user.setId(vendor.getUserId());
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setActive(rs.getBoolean("is_active"));
        vendor.setUser(user);
        
        return vendor;
    }
}
