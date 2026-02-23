package com.trackops.server.adapters.input.web.validation;

import com.trackops.server.domain.model.enums.OrderStatus;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderStatusValidator")
class OrderStatusValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    private OrderStatusValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OrderStatusValidator();
    }

    @Test
    @DisplayName("from PENDING allows CONFIRMED and CANCELLED")
    void fromPendingAllowsConfirmedAndCancelled() {
        ReflectionTestUtils.setField(validator, "fromStatus", "PENDING");
        assertThat(validator.isValid(OrderStatus.CONFIRMED, context)).isTrue();
        assertThat(validator.isValid(OrderStatus.CANCELLED, context)).isTrue();
    }

    @Test
    @DisplayName("from PENDING rejects PROCESSING")
    void fromPendingRejectsProcessing() {
        ReflectionTestUtils.setField(validator, "fromStatus", "PENDING");
        assertThat(validator.isValid(OrderStatus.PROCESSING, context)).isFalse();
    }

    @Test
    @DisplayName("from CONFIRMED allows PROCESSING and CANCELLED")
    void fromConfirmedAllowsProcessingAndCancelled() {
        ReflectionTestUtils.setField(validator, "fromStatus", "CONFIRMED");
        assertThat(validator.isValid(OrderStatus.PROCESSING, context)).isTrue();
        assertThat(validator.isValid(OrderStatus.CANCELLED, context)).isTrue();
    }

    @Test
    @DisplayName("from DELIVERED allows no transitions")
    void fromDeliveredAllowsNone() {
        ReflectionTestUtils.setField(validator, "fromStatus", "DELIVERED");
        assertThat(validator.isValid(OrderStatus.CANCELLED, context)).isFalse();
    }

    @Test
    @DisplayName("null newStatus returns true when fromStatus not specified")
    void nullNewStatusWithEmptyFromReturnsTrue() {
        ReflectionTestUtils.setField(validator, "fromStatus", "");
        assertThat(validator.isValid(null, context)).isTrue();
    }
}
