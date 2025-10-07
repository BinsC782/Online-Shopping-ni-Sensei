package com.shopping.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {

    @Test
    void testIsValidEmail() {
        assertTrue(ValidationUtil.isValidEmail("test@example.com"));
        assertTrue(ValidationUtil.isValidEmail("user.name@sub.domain.co.uk"));
        assertFalse(ValidationUtil.isValidEmail("invalid-email"));
        assertFalse(ValidationUtil.isValidEmail("@example.com"));
        assertFalse(ValidationUtil.isValidEmail(null));
    }

    @Test
    void testIsValidUsername() {
        assertTrue(ValidationUtil.isValidUsername("user123"));
        assertTrue(ValidationUtil.isValidUsername("user_name"));
        assertFalse(ValidationUtil.isValidUsername("us")); // Too short
        assertFalse(ValidationUtil.isValidUsername("user@name")); // Invalid character
        assertFalse(ValidationUtil.isValidUsername(null));
    }

    @Test
    void testIsValidPassword() {
        assertTrue(ValidationUtil.isValidPassword("Passw0rd!"));
        assertFalse(ValidationUtil.isValidPassword("weak")); // Too short
        assertFalse(ValidationUtil.isValidPassword("password")); // No uppercase or special char
        assertFalse(ValidationUtil.isValidPassword("PASSWORD1")); // No lowercase
        assertFalse(ValidationUtil.isValidPassword(null));
    }

    @Test
    void testValidateStringField() {
        // Valid cases
        ValidationUtil.validateStringField("test", "valid", 1, 10);
        
        // Invalid cases
        assertThrows(ValidationUtil.ValidationException.class, 
            () -> ValidationUtil.validateStringField("test", "", 1, 10));
            
        assertThrows(ValidationUtil.ValidationException.class, 
            () -> ValidationUtil.validateStringField("test", "this is too long", 1, 10));
            
        assertThrows(ValidationUtil.ValidationException.class, 
            () -> ValidationUtil.validateStringField("test", null, 1, 10));
    }

    @Test
    void testValidateNumericField() {
        // Valid cases
        ValidationUtil.validateNumericField("test", 5, 1, 10);
        
        // Invalid cases
        assertThrows(ValidationUtil.ValidationException.class, 
            () -> ValidationUtil.validateNumericField("test", 0, 1, 10));
            
        assertThrows(ValidationUtil.ValidationException.class, 
            () -> ValidationUtil.validateNumericField("test", 11, 1, 10));
            
        assertThrows(ValidationUtil.ValidationException.class, 
            () -> ValidationUtil.validateNumericField("test", null, 1, 10));
    }
}
