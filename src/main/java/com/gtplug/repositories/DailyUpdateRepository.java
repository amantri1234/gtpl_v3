package com.gtplug.repositories;

import com.gtplug.config.DatabaseConfig;
import com.gtplug.models.DailyUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DailyUpdateRepository {
    private static final Logger logger = LoggerFactory.getLogger(DailyUpdateRepository.class);
    private final DataSource dataSource;

    public DailyUpdateRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public Optional<DailyUpdate> findById(Long id) {
        String sql = "SELECT du.*, p.project_code, v.vendor_name FROM daily_updates du " +
                     "JOIN projects p ON du.project_id = p.id " +
                     "JOIN vendors v ON du.vendor_id = v.id WHERE du.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToDailyUpdate(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding daily update by ID: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public List<DailyUpdate> findByProjectId(Long projectId, int page, int size) {
        List<DailyUpdate> updates = new ArrayList<>();
        String sql = "SELECT du.*, p.project_code, v.vendor_name FROM daily_updates du " +
                     "JOIN projects p ON du.project_id = p.id " +
                     "JOIN vendors v ON du.vendor_id = v.id " +
                     "WHERE du.project_id = ? ORDER BY du.update_date DESC LIMIT ? OFFSET ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, projectId);
            stmt.setInt(2, size);
            stmt.setInt(3, (page - 1) * size);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                updates.add(mapResultSetToDailyUpdate(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding daily updates by project: {}", e.getMessage());
        }
        return updates;
    }

    public Optional<DailyUpdate> findByProjectIdAndDate(Long projectId, LocalDate date) {
        String sql = "SELECT du.*, p.project_code, v.vendor_name FROM daily_updates du " +
                     "JOIN projects p ON du.project_id = p.id " +
                     "JOIN vendors v ON du.vendor_id = v.id " +
                     "WHERE du.project_id = ? AND du.update_date = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, projectId);
            stmt.setDate(2, Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToDailyUpdate(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding daily update by project and date: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public boolean existsByProjectIdAndDate(Long projectId, LocalDate date) {
        String sql = "SELECT 1 FROM daily_updates WHERE project_id = ? AND update_date = ? LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, projectId);
            stmt.setDate(2, Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Error checking daily update existence: {}", e.getMessage());
        }
        return false;
    }

    public DailyUpdate save(DailyUpdate update) {
        String sql = "INSERT INTO daily_updates (project_id, vendor_id, update_date, km_completed, " +
                     "work_description, photo_proof_path, cumulative_km) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, update.getProjectId());
            stmt.setLong(2, update.getVendorId());
            stmt.setDate(3, Date.valueOf(update.getUpdateDate()));
            stmt.setBigDecimal(4, update.getKmCompleted());
            stmt.setString(5, update.getWorkDescription());
            stmt.setString(6, update.getPhotoProofPath());
            stmt.setBigDecimal(7, update.getCumulativeKm());
            
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                update.setId(rs.getLong(1));
            }
            logger.info("Daily update created for project: {}", update.getProjectId());
        } catch (SQLException e) {
            logger.error("Error creating daily update: {}", e.getMessage());
            throw new RuntimeException("Failed to create daily update", e);
        }
        return update;
    }

    public List<DailyUpdate> findPendingUpdatesByVendor(Long vendorId, LocalDate date) {
        List<DailyUpdate> updates = new ArrayList<>();
        String sql = "SELECT du.*, p.project_code, v.vendor_name FROM daily_updates du " +
                     "JOIN projects p ON du.project_id = p.id " +
                     "JOIN vendors v ON du.vendor_id = v.id " +
                     "WHERE du.vendor_id = ? AND du.update_date < ? AND p.status != 'COMPLETED' " +
                     "ORDER BY du.update_date DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, vendorId);
            stmt.setDate(2, Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                updates.add(mapResultSetToDailyUpdate(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding pending updates: {}", e.getMessage());
        }
        return updates;
    }

    public long countByProjectId(Long projectId) {
        String sql = "SELECT COUNT(*) FROM daily_updates WHERE project_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, projectId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting daily updates: {}", e.getMessage());
        }
        return 0;
    }

    private DailyUpdate mapResultSetToDailyUpdate(ResultSet rs) throws SQLException {
        DailyUpdate update = new DailyUpdate();
        update.setId(rs.getLong("id"));
        update.setProjectId(rs.getLong("project_id"));
        update.setVendorId(rs.getLong("vendor_id"));
        
        Date updateDate = rs.getDate("update_date");
        if (updateDate != null) {
            update.setUpdateDate(updateDate.toLocalDate());
        }
        
        update.setKmCompleted(rs.getBigDecimal("km_completed"));
        update.setWorkDescription(rs.getString("work_description"));
        update.setPhotoProofPath(rs.getString("photo_proof_path"));
        update.setCumulativeKm(rs.getBigDecimal("cumulative_km"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            update.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        // Set transient fields
        var project = new com.gtplug.models.Project();
        project.setId(update.getProjectId());
        project.setProjectCode(rs.getString("project_code"));
        update.setProject(project);
        
        var vendor = new com.gtplug.models.Vendor();
        vendor.setId(update.getVendorId());
        vendor.setVendorName(rs.getString("vendor_name"));
        update.setVendor(vendor);
        
        return update;
    }
}
