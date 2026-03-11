package com.gtplug.services;

import com.gtplug.models.*;
import com.gtplug.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class MaterialRequestService {
    private static final Logger logger = LoggerFactory.getLogger(MaterialRequestService.class);
    private final MaterialRequestRepository materialRequestRepository;
    private final ProjectRepository projectRepository;
    private final NotificationRepository notificationRepository;
    private final AdminLogRepository adminLogRepository;

    public MaterialRequestService() {
        this.materialRequestRepository = new MaterialRequestRepository();
        this.projectRepository = new ProjectRepository();
        this.notificationRepository = new NotificationRepository();
        this.adminLogRepository = new AdminLogRepository();
    }

    public Optional<MaterialRequest> findById(Long id) {
        return materialRequestRepository.findById(id);
    }

    public List<MaterialRequest> findByProjectId(Long projectId) {
        return materialRequestRepository.findByProjectId(projectId);
    }

    public List<MaterialRequest> findByVendorId(Long vendorId) {
        return materialRequestRepository.findByVendorId(vendorId);
    }

    public List<MaterialRequest> findAll(String status, int page, int size) {
        return materialRequestRepository.findAll(status, page, size);
    }

    public long count(String status) {
        return materialRequestRepository.count(status);
    }

    public MaterialRequestResult createRequest(Long projectId, Long vendorId, 
                                                BigDecimal additionalCableKm, 
                                                BigDecimal additionalDuctKm, 
                                                String reason) {
        try {
            // Validate inputs
            boolean hasCable = additionalCableKm != null && additionalCableKm.compareTo(BigDecimal.ZERO) > 0;
            boolean hasDuct = additionalDuctKm != null && additionalDuctKm.compareTo(BigDecimal.ZERO) > 0;
            
            if (!hasCable && !hasDuct) {
                return MaterialRequestResult.failure("Please specify at least one material requirement");
            }
            if (reason == null || reason.trim().isEmpty()) {
                return MaterialRequestResult.failure("Reason is required");
            }

            // Verify project exists and belongs to vendor
            Optional<Project> projectOpt = projectRepository.findById(projectId);
            if (projectOpt.isEmpty()) {
                return MaterialRequestResult.failure("Project not found");
            }

            Project project = projectOpt.get();
            if (!project.getVendorId().equals(vendorId)) {
                return MaterialRequestResult.failure("Project does not belong to this vendor");
            }

            if (project.getStatus() == Project.ProjectStatus.COMPLETED) {
                return MaterialRequestResult.failure("Cannot request materials for completed project");
            }

            // Create request
            MaterialRequest request = new MaterialRequest(projectId, vendorId, 
                additionalCableKm, additionalDuctKm, reason);
            request = materialRequestRepository.save(request);

            // Notify admin
            Notification notification = new Notification(
                project.getAssignedByAdminId(),
                "New Material Request",
                String.format("Vendor has requested materials for project %s", project.getProjectCode()),
                Notification.NotificationType.WARNING,
                project.getId()
            );
            notificationRepository.save(notification);

            logger.info("Material request created for project: {}", projectId);
            return MaterialRequestResult.success(request);

        } catch (Exception e) {
            logger.error("Error creating material request: {}", e.getMessage());
            return MaterialRequestResult.failure("Failed to create request. Please try again.");
        }
    }

    public MaterialRequestResult approveRequest(Long requestId, Long adminId, String remarks,
                                                 String ipAddress, String userAgent) {
        try {
            Optional<MaterialRequest> requestOpt = materialRequestRepository.findById(requestId);
            if (requestOpt.isEmpty()) {
                return MaterialRequestResult.failure("Request not found");
            }

            MaterialRequest request = requestOpt.get();
            if (request.getStatus() != MaterialRequest.RequestStatus.PENDING) {
                return MaterialRequestResult.failure("Request has already been processed");
            }

            request.approve(adminId, remarks);
            request = materialRequestRepository.save(request);

            // Notify vendor
            Optional<Project> projectOpt = projectRepository.findById(request.getProjectId());
            if (projectOpt.isPresent()) {
                Project project = projectOpt.get();
                Notification notification = new Notification(
                    project.getVendor().getUserId(),
                    "Material Request Approved",
                    String.format("Your material request for project %s has been approved", 
                        project.getProjectCode()),
                    Notification.NotificationType.SUCCESS,
                    project.getId()
                );
                notificationRepository.save(notification);
            }

            // Log admin action
            String logDetails = String.format("Admin approved material request %d for project %s", 
                requestId, request.getProject().getProjectCode());
            AdminLog adminLog = new AdminLog(adminId, "APPROVE_MATERIAL_REQUEST", 
                request.getProjectId(), request.getVendorId(), logDetails);
            adminLog.setIpAddress(ipAddress);
            adminLog.setUserAgent(userAgent);
            adminLogRepository.save(adminLog);

            logger.info("Material request approved: {}", requestId);
            return MaterialRequestResult.success(request);

        } catch (Exception e) {
            logger.error("Error approving material request: {}", e.getMessage());
            return MaterialRequestResult.failure("Failed to approve request. Please try again.");
        }
    }

    public MaterialRequestResult rejectRequest(Long requestId, Long adminId, String remarks,
                                                String ipAddress, String userAgent) {
        try {
            Optional<MaterialRequest> requestOpt = materialRequestRepository.findById(requestId);
            if (requestOpt.isEmpty()) {
                return MaterialRequestResult.failure("Request not found");
            }

            MaterialRequest request = requestOpt.get();
            if (request.getStatus() != MaterialRequest.RequestStatus.PENDING) {
                return MaterialRequestResult.failure("Request has already been processed");
            }

            request.reject(adminId, remarks);
            request = materialRequestRepository.save(request);

            // Notify vendor
            Optional<Project> projectOpt = projectRepository.findById(request.getProjectId());
            if (projectOpt.isPresent()) {
                Project project = projectOpt.get();
                Notification notification = new Notification(
                    project.getVendor().getUserId(),
                    "Material Request Rejected",
                    String.format("Your material request for project %s has been rejected. Reason: %s", 
                        project.getProjectCode(), remarks),
                    Notification.NotificationType.ERROR,
                    project.getId()
                );
                notificationRepository.save(notification);
            }

            // Log admin action
            String logDetails = String.format("Admin rejected material request %d for project %s. Reason: %s", 
                requestId, request.getProject().getProjectCode(), remarks);
            AdminLog adminLog = new AdminLog(adminId, "REJECT_MATERIAL_REQUEST", 
                request.getProjectId(), request.getVendorId(), logDetails);
            adminLog.setIpAddress(ipAddress);
            adminLog.setUserAgent(userAgent);
            adminLogRepository.save(adminLog);

            logger.info("Material request rejected: {}", requestId);
            return MaterialRequestResult.success(request);

        } catch (Exception e) {
            logger.error("Error rejecting material request: {}", e.getMessage());
            return MaterialRequestResult.failure("Failed to reject request. Please try again.");
        }
    }

    public static class MaterialRequestResult {
        private final boolean success;
        private final MaterialRequest request;
        private final String message;

        private MaterialRequestResult(boolean success, MaterialRequest request, String message) {
            this.success = success;
            this.request = request;
            this.message = message;
        }

        public static MaterialRequestResult success(MaterialRequest request) {
            return new MaterialRequestResult(true, request, null);
        }

        public static MaterialRequestResult failure(String message) {
            return new MaterialRequestResult(false, null, message);
        }

        public boolean isSuccess() { return success; }
        public MaterialRequest getRequest() { return request; }
        public String getMessage() { return message; }
    }
}
