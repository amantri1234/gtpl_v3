package com.gtplug.repositories;

import com.gtplug.config.DatabaseConfig;
import com.gtplug.models.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectRepository {
    private static final Logger logger = LoggerFactory.getLogger(ProjectRepository.class);
    private final DataSource dataSource;

    public ProjectRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public Optional<Project> findById(Long id) {
        String sql = "SELECT p.*, v.vendor_name, a.full_name as admin_name FROM projects p " +
                     "JOIN vendors v ON p.vendor_id = v.id " +
                     "JOIN admins a ON p.assigned_by_admin_id = a.id " +
                     "WHERE p.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToProject(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding project by ID: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Project> findByProjectCode(String projectCode) {
        String sql = "SELECT p.*, v.vendor_name, a.full_name as admin_name FROM projects p " +
                     "JOIN vendors v ON p.vendor_id = v.id " +
                     "JOIN admins a ON p.assigned_by_admin_id = a.id " +
                     "WHERE p.project_code = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, projectCode);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToProject(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding project by code: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Project save(Project project) {
        if (project.getId() == null) {
            return insert(project);
        } else {
            return update(project);
        }
    }

    private Project insert(Project project) {
        String sql = "INSERT INTO projects (project_code, vendor_id, assigned_by_admin_id, start_location, " +
                     "end_location, total_km, completed_km, remaining_km, cost_per_km, total_cost, " +
                     "work_description, status, start_date, deadline) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, project.getProjectCode());
            stmt.setLong(2, project.getVendorId());
            stmt.setLong(3, project.getAssignedByAdminId());
            stmt.setString(4, project.getStartLocation());
            stmt.setString(5, project.getEndLocation());
            stmt.setBigDecimal(6, project.getTotalKm());
            stmt.setBigDecimal(7, project.getCompletedKm());
            stmt.setBigDecimal(8, project.getRemainingKm());
            stmt.setBigDecimal(9, project.getCostPerKm());
            stmt.setBigDecimal(10, project.getTotalCost());
            stmt.setString(11, project.getWorkDescription());
            stmt.setString(12, project.getStatus().name());
            stmt.setDate(13, Date.valueOf(project.getStartDate()));
            stmt.setDate(14, Date.valueOf(project.getDeadline()));
            
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                project.setId(rs.getLong(1));
            }
            logger.info("Project created: {}", project.getProjectCode());
        } catch (SQLException e) {
            logger.error("Error creating project: {}", e.getMessage());
            throw new RuntimeException("Failed to create project", e);
        }
        return project;
    }

    private Project update(Project project) {
        String sql = "UPDATE projects SET start_location = ?, end_location = ?, total_km = ?, " +
                     "completed_km = ?, remaining_km = ?, cost_per_km = ?, total_cost = ?, " +
                     "work_description = ?, status = ?, start_date = ?, deadline = ?, " +
                     "completed_at = ?, progress_percentage = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, project.getStartLocation());
            stmt.setString(2, project.getEndLocation());
            stmt.setBigDecimal(3, project.getTotalKm());
            stmt.setBigDecimal(4, project.getCompletedKm());
            stmt.setBigDecimal(5, project.getRemainingKm());
            stmt.setBigDecimal(6, project.getCostPerKm());
            stmt.setBigDecimal(7, project.getTotalCost());
            stmt.setString(8, project.getWorkDescription());
            stmt.setString(9, project.getStatus().name());
            stmt.setDate(10, Date.valueOf(project.getStartDate()));
            stmt.setDate(11, Date.valueOf(project.getDeadline()));
            stmt.setTimestamp(12, project.getCompletedAt() != null ? Timestamp.valueOf(project.getCompletedAt()) : null);
            stmt.setInt(13, project.getProgressPercentage());
            stmt.setLong(14, project.getId());
            stmt.executeUpdate();
            logger.debug("Project updated: {}", project.getProjectCode());
        } catch (SQLException e) {
            logger.error("Error updating project: {}", e.getMessage());
            throw new RuntimeException("Failed to update project", e);
        }
        return project;
    }

    public List<Project> findByVendorId(Long vendorId, int page, int size) {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT p.*, v.vendor_name, a.full_name as admin_name FROM projects p " +
                     "JOIN vendors v ON p.vendor_id = v.id " +
                     "JOIN admins a ON p.assigned_by_admin_id = a.id " +
                     "WHERE p.vendor_id = ? ORDER BY p.created_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, vendorId);
            stmt.setInt(2, size);
            stmt.setInt(3, (page - 1) * size);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                projects.add(mapResultSetToProject(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding projects by vendor: {}", e.getMessage());
        }
        return projects;
    }

    public List<Project> findAll(int page, int size, String status, String search) {
        List<Project> projects = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT p.*, v.vendor_name, a.full_name as admin_name FROM projects p " +
            "JOIN vendors v ON p.vendor_id = v.id " +
            "JOIN admins a ON p.assigned_by_admin_id = a.id WHERE 1=1 "
        );
        
        if (status != null && !status.isEmpty()) {
            sql.append("AND p.status = ? ");
        }
        if (search != null && !search.isEmpty()) {
            sql.append("AND (p.project_code LIKE ? OR v.vendor_name LIKE ? OR p.start_location LIKE ?) ");
        }
        sql.append("ORDER BY p.created_at DESC LIMIT ? OFFSET ?");
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (status != null && !status.isEmpty()) {
                stmt.setString(paramIndex++, status);
            }
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
                projects.add(mapResultSetToProject(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all projects: {}", e.getMessage());
        }
        return projects;
    }

    public long count(String status, String search) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM projects p JOIN vendors v ON p.vendor_id = v.id WHERE 1=1 "
        );
        
        if (status != null && !status.isEmpty()) {
            sql.append("AND p.status = ? ");
        }
        if (search != null && !search.isEmpty()) {
            sql.append("AND (p.project_code LIKE ? OR v.vendor_name LIKE ?) ");
        }
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (status != null && !status.isEmpty()) {
                stmt.setString(paramIndex++, status);
            }
            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search + "%";
                stmt.setString(paramIndex++, searchPattern);
                stmt.setString(paramIndex++, searchPattern);
            }
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting projects: {}", e.getMessage());
        }
        return 0;
    }

    public void delete(Long id) {
        String sql = "DELETE FROM projects WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
            logger.info("Project deleted: {}", id);
        } catch (SQLException e) {
            logger.error("Error deleting project: {}", e.getMessage());
            throw new RuntimeException("Failed to delete project", e);
        }
    }

    private Project mapResultSetToProject(ResultSet rs) throws SQLException {
        Project project = new Project();
        project.setId(rs.getLong("id"));
        project.setProjectCode(rs.getString("project_code"));
        project.setVendorId(rs.getLong("vendor_id"));
        project.setAssignedByAdminId(rs.getLong("assigned_by_admin_id"));
        project.setStartLocation(rs.getString("start_location"));
        project.setEndLocation(rs.getString("end_location"));
        project.setTotalKm(rs.getBigDecimal("total_km"));
        project.setCompletedKm(rs.getBigDecimal("completed_km"));
        project.setRemainingKm(rs.getBigDecimal("remaining_km"));
        project.setCostPerKm(rs.getBigDecimal("cost_per_km"));
        project.setTotalCost(rs.getBigDecimal("total_cost"));
        project.setWorkDescription(rs.getString("work_description"));
        project.setStatus(Project.ProjectStatus.valueOf(rs.getString("status")));
        
        Date startDate = rs.getDate("start_date");
        if (startDate != null) {
            project.setStartDate(startDate.toLocalDate());
        }
        
        Date deadline = rs.getDate("deadline");
        if (deadline != null) {
            project.setDeadline(deadline.toLocalDate());
        }
        
        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) {
            project.setCompletedAt(completedAt.toLocalDateTime());
        }
        
        project.setProgressPercentage(rs.getInt("progress_percentage"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            project.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            project.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        // Set transient fields
        var vendor = new com.gtplug.models.Vendor();
        vendor.setId(project.getVendorId());
        vendor.setVendorName(rs.getString("vendor_name"));
        project.setVendor(vendor);
        
        var admin = new com.gtplug.models.Admin();
        admin.setId(project.getAssignedByAdminId());
        admin.setFullName(rs.getString("admin_name"));
        project.setAssignedByAdmin(admin);
        
        return project;
    }
}
