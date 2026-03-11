package com.gtplug.security;

import com.gtplug.config.AppConfig;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.regex.Pattern;

public class PasswordUtil {
    private static final Logger logger = LoggerFactory.getLogger(PasswordUtil.class);
    
    // Password strength requirements
    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\-=\[\]{};':\"\\|,.<>/?]");
    
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Hash a password using BCrypt
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(AppConfig.BCRYPT_ROUNDS));
    }

    /**
     * Verify a password against its hash
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid hash format: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate password strength
     */
    public static PasswordValidationResult validatePasswordStrength(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return new PasswordValidationResult(false, 
                "Password must be at least " + MIN_LENGTH + " characters long");
        }
        
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            return new PasswordValidationResult(false, 
                "Password must contain at least one uppercase letter");
        }
        
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            return new PasswordValidationResult(false, 
                "Password must contain at least one lowercase letter");
        }
        
        if (!DIGIT_PATTERN.matcher(password).find()) {
            return new PasswordValidationResult(false, 
                "Password must contain at least one digit");
        }
        
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            return new PasswordValidationResult(false, 
                "Password must contain at least one special character (!@#$%^&* etc.)");
        }
        
        return new PasswordValidationResult(true, "Password is strong");
    }

    /**
     * Generate a secure random password
     */
    public static String generateSecurePassword(int length) {
        if (length < MIN_LENGTH) {
            length = MIN_LENGTH;
        }
        
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        String all = upper + lower + digits + special;
        
        StringBuilder password = new StringBuilder(length);
        
        // Ensure at least one of each type
        password.append(upper.charAt(secureRandom.nextInt(upper.length())));
        password.append(lower.charAt(secureRandom.nextInt(lower.length())));
        password.append(digits.charAt(secureRandom.nextInt(digits.length())));
        password.append(special.charAt(secureRandom.nextInt(special.length())));
        
        // Fill the rest randomly
        for (int i = 4; i < length; i++) {
            password.append(all.charAt(secureRandom.nextInt(all.length())));
        }
        
        // Shuffle the password
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        
        return new String(chars);
    }

    /**
     * Generate a random token (for CSRF, reset tokens, etc.)
     */
    public static String generateToken(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static class PasswordValidationResult {
        private final boolean valid;
        private final String message;

        public PasswordValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}
