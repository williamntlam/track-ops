package com.trackops.server.adapters.input.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.UUID;

/**
 * Validator for customer ID.
 * Currently performs basic UUID validation, but can be extended
 * to check against a customer service or database.
 */
public class CustomerIdValidator implements ConstraintValidator<ValidCustomerId, UUID> {
    
    @Override
    public void initialize(ValidCustomerId constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(UUID customerId, ConstraintValidatorContext context) {
        if (customerId == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Basic UUID validation (UUID constructor already validates format)
        try {
            // UUID is already validated by the type system
            // Additional business logic can be added here:
            // - Check if customer exists in database
            // - Check if customer is active
            // - Check if customer has permission to create orders
            
            return true; // For now, accept any valid UUID
            
        } catch (Exception e) {
            return false;
        }
    }
}
