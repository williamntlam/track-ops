package com.trackops.server.adapters.input.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure order status transitions are valid.
 * This enforces business rules for order status changes.
 */
@Documented
@Constraint(validatedBy = OrderStatusValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOrderStatus {
    
    String message() default "Invalid order status transition";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * The current status that this new status should transition from.
     * If empty, any status is allowed (for creation).
     */
    String fromStatus() default "";
}
