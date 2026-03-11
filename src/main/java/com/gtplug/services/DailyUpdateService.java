package com.gtplug.services;

import com.gtplug.config.AppConfig;
import com.gtplug.models.*;
import com.gtplug.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DailyUpdateService {
    private static final Logger logger = LoggerFactory.getLogger(DailyUpdateService.class);
    private final DailyUpdateRepository dailyUpdateRepository;
    private final ProjectRepository projectRepository;
    private final NotificationRepository notificationRepository;
    private final VendorRepository vendorRepository;

    public DailyUpdateService() {
        this.dailyUpdateRepository = new DailyUpdateRepository();
        this.projectRepository = new ProjectRepository();
        this.notificationRepository = new NotificationRepository();
        this.vendorRepository = new VendorRepository();
    }

    public Optional<DailyUpdate> findById(Long id) {
        return dailyUpdateRepository.findById(id);
    }

    public List<DailyUpdate> findByProjectId(Long projectId, int page, int size) {
        return dailyUpdateRepository.findByProjectId(projectId, page, size);
    }

    public long countByProjectId(Long projectId) {
        return dailyUpdateRepository.countByProjectId(projectId);
    }

    public DailyUpdateResult submitUpdate(Long projectId, Long vendorId, LocalDate updateDate,
                                           BigDecimal kmCompleted, String workDescription,
                                           byte[] photoData, String photoFilename) {
        try {
            // Validate inputs
            if (kmCompleted.compareTo(BigDecimal.ZERO) <= 0) {
                return DailyUpdateResult.failure("KM completed must be greater than 0");
            }

            // Verify project exists and belongs to vendor
            Optional<Project> projectOpt = projectRepository.findById(projectId);
            if (projectOpt.isEmpty()) {
                return DailyUpdateResult.failure("Project not found");
            }

            Project project = projectOpt.get();
            if (!project.getVendorId().equals(vendorId)) {
                return DailyUpdateResult.failure("Project does not belong to this vendor");
            }

            if (project.getStatus() == Project.ProjectStatus.COMPLETED) {
                return DailyUpdateResult.failure("Project is already completed");
            }

            // Check if update already exists for this date
            Optional<DailyUpdate> existingUpdate = dailyUpdateRepository.findByProjectIdAndDate(projectId, updateDate);
            if (existingUpdate.isPresent()) {
                return DailyUpdateResult.failure("Update already submitted for this date");
            }

            // Calculate cumulative KM
            BigDecimal newCumulativeKm = project.getCompletedKm().add(kmCompleted);
            if (newCumulativeKm.compareTo(project.getTotalKm()) > 0) {
                return DailyUpdateResult.failure(
                    String.format("Total completed KM (%.2f) cannot exceed project total (%.2f)", 
                        newCumulativeKm, project.getTotalKm()));
            }

            // Process and save photo
            String photoPath = null;
            if (photoData != null && photoData.length > 0) {
                if (!AppConfig.isValidImageExtension(photoFilename)) {
                    return DailyUpdateResult.failure("Invalid image format. Allowed: jpg, jpeg, png, gif, webp");
                }
                if (photoData.length > AppConfig.MAX_FILE_SIZE) {
                    return DailyUpdateResult.failure("Image size exceeds maximum allowed (10MB)");
                }
                photoPath = savePhoto(photoData, photoFilename);
            }

            // Create daily update
            DailyUpdate update = new DailyUpdate(projectId, vendorId, updateDate, 
                kmCompleted, workDescription, newCumulativeKm);
            update.setPhotoProofPath(photoPath);
            update = dailyUpdateRepository.save(update);

            // Update project progress (trigger will handle this, but we refresh)
            Optional<Project> updatedProject = projectRepository.findById(projectId);
            if (updatedProject.isPresent()) {
                project = updatedProject.get();
            }

            // Notify admin if project is completed
            if (project.getStatus() == Project.ProjectStatus.COMPLETED) {
                Notification notification = new Notification(
                    project.getAssignedByAdminId(),
                    "Project Completed",
                    String.format("Project %s has been completed by vendor", project.getProjectCode()),
                    Notification.NotificationType.SUCCESS,
                    project.getId()
                );
                notificationRepository.save(notification);
            }

            logger.info("Daily update submitted for project: {}", projectId);
            return DailyUpdateResult.success(update, project);

        } catch (Exception e) {
            logger.error("Error submitting daily update: {}", e.getMessage());
            return DailyUpdateResult.failure("Failed to submit update. Please try again.");
        }
    }

    public boolean hasUpdateForToday(Long projectId) {
        return dailyUpdateRepository.existsByProjectIdAndDate(projectId, LocalDate.now());
    }

    public List<DailyUpdate> getPendingUpdates(Long vendorId) {
        return dailyUpdateRepository.findPendingUpdatesByVendor(vendorId, LocalDate.now());
    }

    private String savePhoto(byte[] photoData, String originalFilename) throws IOException {
        String uploadDir = AppConfig.getUploadPath();
        Path uploadPath = Paths.get(uploadDir);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String filename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(filename);

        // Resize image if needed
        BufferedImage resizedImage = resizeImage(photoData);
        if (resizedImage != null) {
            ImageIO.write(resizedImage, extension.substring(1), filePath.toFile());
        } else {
            Files.write(filePath, photoData);
        }

        return filename;
    }

    private BufferedImage resizeImage(byte[] imageData) {
        try {
            BufferedImage originalImage = ImageIO.read(new java.io.ByteArrayInputStream(imageData));
            if (originalImage == null) {
                return null;
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            if (width <= AppConfig.MAX_IMAGE_WIDTH && height <= AppConfig.MAX_IMAGE_HEIGHT) {
                return originalImage;
            }

            double scale = Math.min(
                (double) AppConfig.MAX_IMAGE_WIDTH / width,
                (double) AppConfig.MAX_IMAGE_HEIGHT / height
            );

            int newWidth = (int) (width * scale);
            int newHeight = (int) (height * scale);

            Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(scaledImage, 0, 0, null);
            g.dispose();

            return resizedImage;
        } catch (IOException e) {
            logger.error("Error resizing image: {}", e.getMessage());
            return null;
        }
    }

    public static class DailyUpdateResult {
        private final boolean success;
        private final DailyUpdate update;
        private final Project updatedProject;
        private final String message;

        private DailyUpdateResult(boolean success, DailyUpdate update, Project updatedProject, String message) {
            this.success = success;
            this.update = update;
            this.updatedProject = updatedProject;
            this.message = message;
        }

        public static DailyUpdateResult success(DailyUpdate update, Project updatedProject) {
            return new DailyUpdateResult(true, update, updatedProject, null);
        }

        public static DailyUpdateResult failure(String message) {
            return new DailyUpdateResult(false, null, null, message);
        }

        public boolean isSuccess() { return success; }
        public DailyUpdate getUpdate() { return update; }
        public Project getUpdatedProject() { return updatedProject; }
        public String getMessage() { return message; }
    }
}
