package com.gtplug.repositories;

import com.gtplug.config.DatabaseConfig;
import com.gtplug.models.MaterialRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MaterialRequestRepository {
    private static final Logger logger = LoggerFactory.getLogger(MaterialRequestRepository.class);
    private final DataSource dataSource;

    public MaterialRequestRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public Optional<MaterialRequest> findById(Long id) {
        String sql = "SELECT mr.*, p.project_code, v.vendor_name, a.full_name as admin_name " +
                     "FROM material_requests mr " +
                     "JOIN projects p ON mr.project_id = p.id " +
                     "JOIN vendors v ON mr.vendor_id = v.id " +
                     "LEFT JOIN admins a ON mr.approved_by_admin_id = a.id WHERE mr.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToMaterialRequest(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding material request by ID: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public List<MaterialRequest> findByProjectId(Long projectId) {
        List<MaterialRequest> requests = new ArrayList<>();
        String sql = "SELECT mr.*, p.project_code, v.vendor_name, a.full_name as admin_name " +
                     "FROM material_requests mr " +
                     "JOIN projects p ON mr.project_id = p.id " +
                     "JOIN vendors v ON mr.vendor_id = v.id " +
                     "LEFT JOIN admins a ON mr.approved_by_admin_id = a.id " +
                     "WHERE mr.project_id = ? ORDER BY mr.requested_at DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, projectId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                requests.add(mapResultSetToMaterialRequest(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding material requests by project: {}", e.getMessage());
        }
        return requests;
    }

    public List<MaterialRequest> findByVendorId(Long vendorId) {
        List<MaterialRequest> requests = new ArrayList<>();
        String sql = "SELECT mr.*, p.project_code, v.vendor_name, a.full_name as admin_name " +
                     "FROM material_requests mr " +
                     "JOIN projects p ON mr.project_id = p.id " +
                     "JOIN vendors v ON mr.vendor_id = v.id " +
                     "LEFT JOIN admins a ON mr.approved_by_admin_id = a.id " +
                     "WHERE mr.vendor_id = ? ORDER BY mr.requested_at DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, vendorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                requests.add(mapResultSetToMaterialRequest(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding material requests by vendor: {}", e.getMessage());
        }
        return requests;
    }

    public MaterialRequest save(MaterialRequest request) {
        if (request.getId() == null) {
            return insert(request);
        } else {
            return update(request);
        }
    }

    private MaterialRequest insert(MaterialRequest request) {
        String sql = "INSERT INTO material_requests (project_id, vendor_id, additional_cable_km, " +
                     "additional_duct_km, reason, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, request.getProjectId());
            stmt.setLong(2, request.getVendorId());
            stmt.setBigDecimal(3, request.getAdditionalCableKm());
            stmt.setBigDecimal(4, request.getAdditionalDuctKm());
            stmt.setString(5, request.getReason());
            stmt.setString(6, request.getStatus().name());
            
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                request.setId(rs.getLong(1));
            }
            logger.info("Material request created for project: {}", request.getProjectId());
        } catch (SQLException e) {
            logger.error("Error creating material request: {}", e.getMessage());
            throw new RuntimeException("Failed to create material request", e);
        }
        return request;
    }

    private MaterialRequest update(MaterialRequest request) {
        String sql = "UPDATE material_requests SET status = ?, approved_by_admin_id = ?, " +
                     "approved_at = ?, admin_remarks = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, request.getStatus().name());
            stmt.setObject(2, request.getApprovedByAdminId(), Types.BIGINT);
            stmt.setTimestamp(3, request.getApprovedAt() != null ? Timestamp.valueOf(request.getApprovedAt()) : null);
            stmt.setString(4, request.getAdminRemarks());
            stmt.setLong(5, request.getId());
            stmt.executeUpdate();
            logger.debug("Material request updated: {}", request.getId());
        } catch (SQLException e) {
            logger.error("Error updating material request: {}", e.getMessage());
            throw new RuntimeException("Failed to update material request", e);
        }
        return request;
    }

    public List<MaterialRequest> findAll(String status, int page, int size) {
        List<MaterialRequest> requests = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT mr.*, p.project_code, v.vendor_name, a.full_name as admin_name " +
            "FROM material_requests mr " +
            "JOIN projects p ON mr.project_id = p.id " +
            "JOIN vendors v ON mr.vendor_id = v.id " +
            "LEFT JOIN admins a ON mr.approved_by_admin_id = a.id WHERE 1=1 "
        );
        
        if (status != null && !status.isEmpty()) {
            sql.append("AND mr.status = ? ");
        }
        sql.append("ORDER BY mr.requested_at DESC LIMIT ? OFFSET ?");
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (status != null && !status.isEmpty()) {
                stmt.setString(paramIndex++, status);
            }
            stmt.setInt(paramIndex++, size);
            stmt.setInt(paramIndex, (page - 1) * size);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                requests.add(mapResultSetToMaterialRequest(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all material requests: {}", e.getMessage());
        }
        return requests;
    }

    public long count(String status) {
        String sql = "SELECT COUNT(*) FROM material_requests WHERE 1=1 " +
                     (status != null && !status.isEmpty() ? "AND status = ?" : "");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (status != null && !status.isEmpty()) {
                stmt.setString(1, status);
            }
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting material requests: {}", e.getMessage());
        }
        return 0;
    }

    private MaterialRequest mapResultSetToMaterialRequest(ResultSet rs) throws SQLException {
        MaterialRequest request = new MaterialRequest();
        request.setId(rs.getLong("id"));
        request.setProjectId(rs.getLong("project_id"));
        request.setVendorId(rs.getLong("vendor_id"));
        request.setAdditionalCableKm(rs.getBigDecimal("additional_cable_km"));
        request.setAdditionalDuctKm(rs.getBigDecimal("additional_duct_km"));
        request.setReason(rs.getString("reason"));
        request.setStatus(MaterialRequest.RequestStatus.valueOf(rs.getString("status")));
        
        Timestamp requestedAt = rs.getTimestamp("requested_at");
        if (requestedAt != null) {
            request.setRequestedAt(requestedAt.toLocalDateTime());
        }
        
        long approvedById = rs.getLong("approved_by_admin_id");
        if (!rs.wasNull()) {
            request.setApprovedByAdminId(approvedById);
        }
        
        Timestamp approvedAt = rs.getTimestamp("approved_at");
        if (approvedAt != null) {
            request.setApprovedAt(approvedAt.toLocalDateTime());
        }
        
        request.setAdminRemarks(rs.getString("admin_remarks"));
        
        // Set transient fields
        var project = new com.gtplug.models.Project();
        project.setId(request.getProjectId());
        project.setProjectCode(rs.getString("project_code"));
        request.setProject(project);
        
        var vendor = new com.gtplug.models.Vendor();
        vendor.setId(request.getVendorId());
        vendor.setVendorName(rs.getString("vendor_name"));
        request.setVendor(vendor);
        
        String adminName = rs.getString("admin_name");
        if (adminName != null) {
            var admin = new com.gtplug.models.Admin();
            admin.setId(request.getApprovedByAdminId());
            admin.setFullName(adminName);
            request.setApprovedByAdmin(admin);
        }
        
        return request;
    }
}
