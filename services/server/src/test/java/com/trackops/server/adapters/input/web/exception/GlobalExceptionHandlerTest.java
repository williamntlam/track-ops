package com.trackops.server.adapters.input.web.exception;

import com.trackops.server.domain.exceptions.InvalidOrderStatusTransitionException;
import com.trackops.server.domain.exceptions.OrderNotFoundException;
import com.trackops.server.domain.exceptions.OrderValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/orders");
    }

    @Nested
    @DisplayName("OrderNotFoundException")
    class OrderNotFound {

        @Test
        @DisplayName("returns 404 and error body")
        void returns404() {
            UUID id = UUID.randomUUID();
            ResponseEntity<ErrorResponse> res = handler.handleOrderNotFoundException(
                    new OrderNotFoundException(id), request);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(res.getBody()).isNotNull();
            assertThat(res.getBody().getStatus()).isEqualTo(404);
            assertThat(res.getBody().getError()).isEqualTo("Order Not Found");
            assertThat(res.getBody().getMessage()).contains(id.toString());
        }
    }

    @Nested
    @DisplayName("OrderValidationException")
    class OrderValidation {

        @Test
        @DisplayName("returns 400 and error body")
        void returns400() {
            ResponseEntity<ErrorResponse> res = handler.handleOrderValidationException(
                    new OrderValidationException("Invalid amount"), request);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(res.getBody()).isNotNull();
            assertThat(res.getBody().getStatus()).isEqualTo(400);
            assertThat(res.getBody().getMessage()).isEqualTo("Invalid amount");
        }
    }

    @Nested
    @DisplayName("InvalidOrderStatusTransitionException")
    class InvalidStatusTransition {

        @Test
        @DisplayName("returns 409 and error body")
        void returns409() {
            ResponseEntity<ErrorResponse> res = handler.handleInvalidOrderStatusTransitionException(
                    new InvalidOrderStatusTransitionException("Cannot cancel delivered order"), request);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(res.getBody()).isNotNull();
            assertThat(res.getBody().getStatus()).isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException")
    class ValidationExceptions {

        @Test
        @DisplayName("returns 400 with validation errors")
        void returns400WithFieldErrors() {
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(
                    java.util.List.of(new FieldError("request", "customerId", "must not be null")));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<ErrorResponse> res = handler.handleValidationExceptions(ex, request);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(res.getBody()).isNotNull();
            assertThat(res.getBody().getValidationErrors()).hasSize(1);
            assertThat(res.getBody().getValidationErrors().get(0).getField()).isEqualTo("customerId");
        }
    }

    @Nested
    @DisplayName("HttpMessageNotReadableException")
    class MalformedRequest {

        @Test
        @DisplayName("returns 400")
        void returns400() {
            ResponseEntity<ErrorResponse> res = handler.handleHttpMessageNotReadableException(
                    new HttpMessageNotReadableException("Invalid JSON", (Throwable) null, null), request);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(res.getBody().getError()).isEqualTo("Malformed Request");
        }
    }

    @Nested
    @DisplayName("RuntimeException")
    class Runtime {

        @Test
        @DisplayName("returns 500")
        void returns500() {
            ResponseEntity<ErrorResponse> res = handler.handleRuntimeException(
                    new RuntimeException("unexpected"), request);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(res.getBody().getStatus()).isEqualTo(500);
        }
    }
}
