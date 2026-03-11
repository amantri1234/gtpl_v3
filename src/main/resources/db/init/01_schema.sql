-- GTPLUG Underground Work Management System - Database Schema
-- MySQL 8.0

-- Use the application database
USE gtplug_db;

-- ============================================
-- USERS TABLE (Base table for authentication)
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'VENDOR') NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- ADMINS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    mobile_number VARCHAR(15),
    department VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- VENDORS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS vendors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vendor_name VARCHAR(100) NOT NULL,
    mobile_number VARCHAR(15) NOT NULL,
    registration_date DATE NOT NULL,
    total_projects INT DEFAULT 0,
    active_projects INT DEFAULT 0,
    completed_projects INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_vendor_name (vendor_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- PROJECTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_code VARCHAR(20) NOT NULL UNIQUE,
    vendor_id BIGINT NOT NULL,
    assigned_by_admin_id BIGINT NOT NULL,
    start_location VARCHAR(255) NOT NULL,
    end_location VARCHAR(255) NOT NULL,
    total_km DECIMAL(10, 2) NOT NULL,
    completed_km DECIMAL(10, 2) DEFAULT 0.00,
    remaining_km DECIMAL(10, 2) NOT NULL,
    cost_per_km DECIMAL(12, 2) NOT NULL,
    total_cost DECIMAL(15, 2) NOT NULL,
    work_description TEXT,
    status ENUM('ASSIGNED', 'IN_PROGRESS', 'DELAYED', 'COMPLETED') DEFAULT 'ASSIGNED',
    start_date DATE NOT NULL,
    deadline DATE NOT NULL,
    completed_at TIMESTAMP NULL,
    progress_percentage INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE RESTRICT,
    FOREIGN KEY (assigned_by_admin_id) REFERENCES admins(id) ON DELETE RESTRICT,
    INDEX idx_vendor_id (vendor_id),
    INDEX idx_status (status),
    INDEX idx_project_code (project_code),
    INDEX idx_deadline (deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- DAILY UPDATES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS daily_updates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    update_date DATE NOT NULL,
    km_completed DECIMAL(10, 2) NOT NULL,
    work_description TEXT NOT NULL,
    photo_proof_path VARCHAR(500),
    cumulative_km DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE RESTRICT,
    INDEX idx_project_id (project_id),
    INDEX idx_vendor_id (vendor_id),
    INDEX idx_update_date (update_date),
    UNIQUE KEY unique_daily_update (project_id, update_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- MATERIAL REQUESTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS material_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    additional_cable_km DECIMAL(10, 2) DEFAULT 0.00,
    additional_duct_km DECIMAL(10, 2) DEFAULT 0.00,
    reason TEXT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_by_admin_id BIGINT NULL,
    approved_at TIMESTAMP NULL,
    admin_remarks TEXT,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE RESTRICT,
    FOREIGN KEY (approved_by_admin_id) REFERENCES admins(id) ON DELETE SET NULL,
    INDEX idx_project_id (project_id),
    INDEX idx_vendor_id (vendor_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- ADMIN LOGS TABLE (Activity Logging)
-- ============================================
CREATE TABLE IF NOT EXISTS admin_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    project_id BIGINT NULL,
    vendor_id BIGINT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE SET NULL,
    INDEX idx_admin_id (admin_id),
    INDEX idx_created_at (created_at),
    INDEX idx_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- NOTIFICATIONS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('INFO', 'WARNING', 'SUCCESS', 'ERROR') DEFAULT 'INFO',
    is_read BOOLEAN DEFAULT FALSE,
    related_project_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (related_project_id) REFERENCES projects(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- CSRF TOKENS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS csrf_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- RATE LIMITING TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS rate_limits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    identifier VARCHAR(255) NOT NULL,
    endpoint VARCHAR(100) NOT NULL,
    request_count INT DEFAULT 1,
    window_start TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_rate_limit (identifier, endpoint),
    INDEX idx_identifier (identifier),
    INDEX idx_window_start (window_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- INSERT DEFAULT ADMIN USER
-- Password: admin123 (BCrypt hashed)
-- ============================================
INSERT IGNORE INTO users (id, username, email, password_hash, role, is_active) 
VALUES (1, 'admin', 'admin@gtplug.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQzBZN0UfGNEKjN.mQhJg8J7.hT2', 'ADMIN', TRUE);

INSERT IGNORE INTO admins (id, user_id, full_name, mobile_number, department) 
VALUES (1, 1, 'System Administrator', '9999999999', 'IT Administration');

-- ============================================
-- INSERT SAMPLE VENDOR USER (for testing)
-- Password: vendor123 (BCrypt hashed)
-- ============================================
INSERT IGNORE INTO users (id, username, email, password_hash, role, is_active) 
VALUES (2, 'vendor1', 'vendor1@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQzBZN0UfGNEKjN.mQhJg8J7.hT2', 'VENDOR', TRUE);

INSERT IGNORE INTO vendors (id, user_id, vendor_name, mobile_number, registration_date) 
VALUES (1, 2, 'ABC Cable Works', '8888888888', CURDATE());

-- ============================================
-- CREATE TRIGGERS FOR AUTOMATIC UPDATES
-- ============================================

DELIMITER //

-- Trigger to update project progress when daily update is added
CREATE TRIGGER IF NOT EXISTS update_project_progress
AFTER INSERT ON daily_updates
FOR EACH ROW
BEGIN
    DECLARE total_completed DECIMAL(10, 2);
    DECLARE project_total DECIMAL(10, 2);
    DECLARE new_status VARCHAR(20);
    
    SELECT SUM(km_completed) INTO total_completed 
    FROM daily_updates 
    WHERE project_id = NEW.project_id;
    
    SELECT total_km INTO project_total FROM projects WHERE id = NEW.project_id;
    
    SET new_status = CASE
        WHEN total_completed >= project_total THEN 'COMPLETED'
        WHEN total_completed > 0 THEN 'IN_PROGRESS'
        ELSE 'ASSIGNED'
    END;
    
    UPDATE projects 
    SET completed_km = total_completed,
        remaining_km = project_total - total_completed,
        progress_percentage = LEAST(ROUND((total_completed / project_total) * 100), 100),
        status = new_status,
        completed_at = CASE WHEN total_completed >= project_total THEN NOW() ELSE NULL END
    WHERE id = NEW.project_id;
END //

-- Trigger to update vendor project counts
CREATE TRIGGER IF NOT EXISTS update_vendor_project_counts
AFTER UPDATE ON projects
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        UPDATE vendors v
        SET 
            active_projects = (
                SELECT COUNT(*) FROM projects 
                WHERE vendor_id = v.id AND status IN ('ASSIGNED', 'IN_PROGRESS', 'DELAYED')
            ),
            completed_projects = (
                SELECT COUNT(*) FROM projects 
                WHERE vendor_id = v.id AND status = 'COMPLETED'
            ),
            total_projects = (
                SELECT COUNT(*) FROM projects WHERE vendor_id = v.id
            )
        WHERE v.id = NEW.vendor_id;
    END IF;
END //

-- Trigger to clean expired CSRF tokens
CREATE TRIGGER IF NOT EXISTS cleanup_expired_csrf
AFTER INSERT ON csrf_tokens
FOR EACH ROW
BEGIN
    DELETE FROM csrf_tokens WHERE expires_at < NOW();
END //

-- Trigger to clean old rate limit entries
CREATE TRIGGER IF NOT EXISTS cleanup_rate_limits
AFTER INSERT ON rate_limits
FOR EACH ROW
BEGIN
    DELETE FROM rate_limits WHERE window_start < DATE_SUB(NOW(), INTERVAL 1 HOUR);
END //

DELIMITER ;
