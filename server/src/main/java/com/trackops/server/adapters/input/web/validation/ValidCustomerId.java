package com.trackops.server.adapters.input.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure customer ID is valid.
 * This can be extended to check against a customer service or database.
 */
@Documented
@Constraint(validatedBy = CustomerIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCustomerId {
    
    String message() default "Invalid customer ID";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
