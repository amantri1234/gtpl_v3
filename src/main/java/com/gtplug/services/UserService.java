package com.gtplug.services;

import com.gtplug.config.DatabaseConfig;
import com.gtplug.models.User;
import com.gtplug.repositories.UserRepository;
import com.gtplug.security.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService() {
        this.userRepository = new UserRepository();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User createUser(String username, String email, String password, User.UserRole role) {
        // Validate password strength
        var validation = PasswordUtil.validatePasswordStrength(password);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getMessage());
        }

        // Check uniqueness
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Hash password and create user
        String passwordHash = PasswordUtil.hashPassword(password);
        User user = new User(username, email, passwordHash, role);
        
        return userRepository.save(user);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.delete(id);
    }

    public AuthenticationResult authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            logger.warn("Authentication failed: User not found - {}", username);
            return AuthenticationResult.failure("Invalid username or password");
        }

        User user = userOpt.get();

        // Check if account is locked
        if (user.isLocked()) {
            logger.warn("Authentication failed: Account locked - {}", username);
            return AuthenticationResult.failure("Account is temporarily locked. Please try again later.");
        }

        // Check if account is active
        if (!user.isActive()) {
            logger.warn("Authentication failed: Account inactive - {}", username);
            return AuthenticationResult.failure("Account is deactivated");
        }

        // Verify password
        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            // Increment failed attempts
            user.incrementFailedAttempts();
            
            // Lock account if too many failed attempts
            int maxAttempts = DatabaseConfig.getMaxFailedLoginAttempts();
            if (user.getFailedLoginAttempts() >= maxAttempts) {
                LocalDateTime lockUntil = LocalDateTime.now().plusSeconds(
                    DatabaseConfig.getAccountLockDuration() / 1000
                );
                user.setLockedUntil(lockUntil);
                userRepository.lockAccount(user.getId(), lockUntil);
                logger.warn("Account locked due to too many failed attempts: {}", username);
                return AuthenticationResult.failure("Account locked due to too many failed attempts. Try again in 30 minutes.");
            }
            
            userRepository.incrementFailedAttempts(user.getId());
            int remainingAttempts = maxAttempts - user.getFailedLoginAttempts();
            logger.warn("Authentication failed: Invalid password - {} ({} attempts remaining)", 
                username, remainingAttempts);
            return AuthenticationResult.failure("Invalid username or password. " + 
                (remainingAttempts > 0 ? remainingAttempts + " attempts remaining." : ""));
        }

        // Successful authentication
        user.resetFailedAttempts();
        userRepository.updateLastLogin(user.getId());
        
        logger.info("Authentication successful: {}", username);
        return AuthenticationResult.success(user);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();

        // Verify current password
        if (!PasswordUtil.verifyPassword(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Validate new password strength
        var validation = PasswordUtil.validatePasswordStrength(newPassword);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getMessage());
        }

        // Update password
        user.setPasswordHash(PasswordUtil.hashPassword(newPassword));
        userRepository.save(user);
        
        logger.info("Password changed for user: {}", user.getUsername());
    }

    public void deactivateUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setActive(false);
            userRepository.save(user);
            logger.info("User deactivated: {}", user.getUsername());
        }
    }

    public void activateUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setActive(true);
            userRepository.save(user);
            logger.info("User activated: {}", user.getUsername());
        }
    }

    public static class AuthenticationResult {
        private final boolean success;
        private final User user;
        private final String message;

        private AuthenticationResult(boolean success, User user, String message) {
            this.success = success;
            this.user = user;
            this.message = message;
        }

        public static AuthenticationResult success(User user) {
            return new AuthenticationResult(true, user, null);
        }

        public static AuthenticationResult failure(String message) {
            return new AuthenticationResult(false, null, message);
        }

        public boolean isSuccess() { return success; }
        public User getUser() { return user; }
        public String getMessage() { return message; }
    }
}
