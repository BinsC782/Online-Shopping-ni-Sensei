package com.shopping.util;

import java.util.regex.Pattern;

/**
 * Utility class for input validation
 */
public class ValidationUtil {
    // Common patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_]{3,30}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");
    
    // Validation methods
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }
    
    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }
    
    public static boolean isValidId(String id) {
        return id != null && !id.trim().isEmpty();
    }
    
    public static boolean isValidQuantity(int quantity) {
        return quantity > 0 && quantity <= 1000; // Reasonable upper limit
    }
    
    public static boolean isValidPrice(double price) {
        return price >= 0 && price <= 1_000_000; // Reasonable upper limit
    }
    
    public static void validateStringField(String fieldName, String value, int minLength, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty");
        }
        if (value.length() < minLength || value.length() > maxLength) {
            throw new ValidationException(fieldName + " must be between " + minLength + " and " + maxLength + " characters");
        }
    }
    
    public static void validateNumericField(String fieldName, Number value, Number min, Number max) {
        if (value == null) {
            throw new ValidationException(fieldName + " cannot be null");
        }
        double val = value.doubleValue();
        if (val < min.doubleValue() || val > max.doubleValue()) {
            throw new ValidationException(fieldName + " must be between " + min + " and " + max);
        }
    }
    
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}
