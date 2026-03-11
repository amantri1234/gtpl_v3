package com.gtplug.services;

import com.gtplug.models.*;
import com.gtplug.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ProjectService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);
    private final ProjectRepository projectRepository;
    private final VendorRepository vendorRepository;
    private final AdminRepository adminRepository;
    private final NotificationRepository notificationRepository;
    private final AdminLogRepository adminLogRepository;
    private final AtomicInteger projectCounter;

    public ProjectService() {
        this.projectRepository = new ProjectRepository();
        this.vendorRepository = new VendorRepository();
        this.adminRepository = new AdminRepository();
        this.notificationRepository = new NotificationRepository();
        this.adminLogRepository = new AdminLogRepository();
        this.projectCounter = new AtomicInteger(100);
    }

    public Optional<Project> findById(Long id) {
        return projectRepository.findById(id);
    }

    public Optional<Project> findByProjectCode(String projectCode) {
        return projectRepository.findByProjectCode(projectCode);
    }

    public List<Project> findByVendorId(Long vendorId, int page, int size) {
        return projectRepository.findByVendorId(vendorId, page, size);
    }

    public List<Project> findAll(int page, int size, String status, String search) {
        return projectRepository.findAll(page, size, status, search);
    }

    public long count(String status, String search) {
        return projectRepository.count(status, search);
    }

    public ProjectAssignmentResult assignProject(Long vendorId, Long adminId, String startLocation,
                                                  String endLocation, BigDecimal totalKm, 
                                                  BigDecimal costPerKm, String workDescription,
                                                  LocalDate startDate, LocalDate deadline,
                                                  String adminName, String ipAddress, String userAgent) {
        try {
            // Validate inputs
            if (totalKm.compareTo(BigDecimal.ZERO) <= 0) {
                return ProjectAssignmentResult.failure("Total kilometers must be greater than 0");
            }
            if (costPerKm.compareTo(BigDecimal.ZERO) <= 0) {
                return ProjectAssignmentResult.failure("Cost per kilometer must be greater than 0");
            }
            if (deadline.isBefore(startDate)) {
                return ProjectAssignmentResult.failure("Deadline must be after start date");
            }

            // Verify vendor exists
            Optional<Vendor> vendorOpt = vendorRepository.findById(vendorId);
            if (vendorOpt.isEmpty()) {
                return ProjectAssignmentResult.failure("Vendor not found");
            }

            // Verify admin exists
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return ProjectAssignmentResult.failure("Admin not found");
            }

            // Generate project code
            String projectCode = generateProjectCode();

            // Create project
            Project project = new Project(projectCode, vendorId, adminId, startLocation, 
                endLocation, totalKm, costPerKm, workDescription, startDate, deadline);
            project = projectRepository.save(project);

            // Create notification for vendor
            Vendor vendor = vendorOpt.get();
            Notification notification = new Notification(
                vendor.getUserId(),
                "New Project Assigned",
                "You have been assigned a new project: " + projectCode,
                Notification.NotificationType.INFO,
                project.getId()
            );
            notificationRepository.save(notification);

            // Log admin action
            String logDetails = String.format("Admin %s assigned project %s to vendor %s", 
                adminName, projectCode, vendor.getVendorName());
            AdminLog adminLog = new AdminLog(adminId, "ASSIGN_PROJECT", project.getId(), vendorId, logDetails);
            adminLog.setIpAddress(ipAddress);
            adminLog.setUserAgent(userAgent);
            adminLogRepository.save(adminLog);

            logger.info("Project assigned: {} to vendor: {}", projectCode, vendor.getVendorName());
            return ProjectAssignmentResult.success(project);

        } catch (Exception e) {
            logger.error("Error assigning project: {}", e.getMessage());
            return ProjectAssignmentResult.failure("Failed to assign project. Please try again.");
        }
    }

    public void updateProjectStatus(Long projectId) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            project.updateStatus();
            projectRepository.save(project);
        }
    }

    public void deleteProject(Long id) {
        projectRepository.delete(id);
    }

    private String generateProjectCode() {
        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yy"));
        int sequence = projectCounter.incrementAndGet();
        return "UG" + year + String.format("%05d", sequence);
    }

    public static class ProjectAssignmentResult {
        private final boolean success;
        private final Project project;
        private final String message;

        private ProjectAssignmentResult(boolean success, Project project, String message) {
            this.success = success;
            this.project = project;
            this.message = message;
        }

        public static ProjectAssignmentResult success(Project project) {
            return new ProjectAssignmentResult(true, project, null);
        }

        public static ProjectAssignmentResult failure(String message) {
            return new ProjectAssignmentResult(false, null, message);
        }

        public boolean isSuccess() { return success; }
        public Project getProject() { return project; }
        public String getMessage() { return message; }
    }
}
