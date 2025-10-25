package com.trackops.server.adapters.input.web.validation;

import com.trackops.server.domain.model.enums.OrderStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.EnumSet;
import java.util.Set;

/**
 * Validator for order status transitions.
 * Enforces business rules for valid order status changes.
 */
public class OrderStatusValidator implements ConstraintValidator<ValidOrderStatus, OrderStatus> {
    
    private String fromStatus;
    
    // Define valid status transitions
    private static final Set<OrderStatus> FROM_PENDING = EnumSet.of(
        OrderStatus.CONFIRMED, OrderStatus.CANCELLED
    );
    
    private static final Set<OrderStatus> FROM_CONFIRMED = EnumSet.of(
        OrderStatus.PROCESSING, OrderStatus.CANCELLED
    );
    
    private static final Set<OrderStatus> FROM_PROCESSING = EnumSet.of(
        OrderStatus.SHIPPED, OrderStatus.CANCELLED
    );
    
    private static final Set<OrderStatus> FROM_SHIPPED = EnumSet.of(
        OrderStatus.DELIVERED, OrderStatus.CANCELLED
    );
    
    // Terminal states - no transitions allowed
    private static final Set<OrderStatus> TERMINAL_STATES = EnumSet.of(
        OrderStatus.DELIVERED, OrderStatus.CANCELLED
    );
    
    @Override
    public void initialize(ValidOrderStatus constraintAnnotation) {
        this.fromStatus = constraintAnnotation.fromStatus();
    }
    
    @Override
    public boolean isValid(OrderStatus newStatus, ConstraintValidatorContext context) {
        if (newStatus == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // If no fromStatus specified, allow any status (for creation)
        if (fromStatus == null || fromStatus.isEmpty()) {
            return true;
        }
        
        try {
            OrderStatus currentStatus = OrderStatus.valueOf(fromStatus.toUpperCase());
            
            // Check if current status is terminal
            if (TERMINAL_STATES.contains(currentStatus)) {
                return false; // No transitions from terminal states
            }
            
            // Check valid transitions based on current status
            Set<OrderStatus> validTransitions = getValidTransitions(currentStatus);
            return validTransitions.contains(newStatus);
            
        } catch (IllegalArgumentException e) {
            // Invalid fromStatus - let other validators handle this
            return true;
        }
    }
    
    private Set<OrderStatus> getValidTransitions(OrderStatus currentStatus) {
        return switch (currentStatus) {
            case PENDING -> FROM_PENDING;
            case CONFIRMED -> FROM_CONFIRMED;
            case PROCESSING -> FROM_PROCESSING;
            case SHIPPED -> FROM_SHIPPED;
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class); // Terminal states
        };
    }
}
