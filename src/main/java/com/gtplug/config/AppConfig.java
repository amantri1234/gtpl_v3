package com.gtplug.config;

import java.io.File;

public class AppConfig {
    
    public static final String APP_NAME = "GTPLUG Underground Work Management System";
    public static final String APP_VERSION = "1.0.0";
    
    // Server Configuration
    public static final int SERVER_PORT = Integer.parseInt(
        System.getenv().getOrDefault("SERVER_PORT", "8080"));
    public static final String SERVER_HOST = 
        System.getenv().getOrDefault("SERVER_HOST", "0.0.0.0");
    
    // Security Configuration
    public static final int BCRYPT_ROUNDS = 10;
    public static final boolean CSRF_ENABLED = Boolean.parseBoolean(
        System.getenv().getOrDefault("CSRF_ENABLED", "true"));
    
    // Upload Configuration
    public static final long MAX_FILE_SIZE = Long.parseLong(
        System.getenv().getOrDefault("MAX_FILE_SIZE", "10485760")); // 10MB
    public static final String[] ALLOWED_EXTENSIONS = 
        System.getenv().getOrDefault("ALLOWED_EXTENSIONS", "jpg,jpeg,png,gif,webp").split(",");
    public static final int MAX_IMAGE_WIDTH = 2048;
    public static final int MAX_IMAGE_HEIGHT = 2048;
    
    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    
    // Date/Time Format
    public static final String DATE_FORMAT = "dd-MM-yyyy";
    public static final String DATETIME_FORMAT = "dd-MM-yyyy HH:mm:ss";
    
    static {
        // Ensure upload directory exists
        String uploadDir = DatabaseConfig.getUploadDirectory();
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    public static String getUploadPath() {
        return DatabaseConfig.getUploadDirectory();
    }
    
    public static boolean isValidImageExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.trim().equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }
}
