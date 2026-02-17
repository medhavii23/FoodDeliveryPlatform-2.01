package com.foodapp.delivery_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;


import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/delivery/estimate");
    }

    @Test
    void handleDeliveryNotFound_returns404() {
        DeliveryNotFoundException ex = new DeliveryNotFoundException(java.util.UUID.randomUUID());

        ResponseEntity<ApiError> response = handler.handleDeliveryNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Delivery not found");
    }

    @Test
    void handleValidation_returns400_withFieldErrors() throws Exception {
        // Create a binding result with 2 field errors
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "deliveryAssignRequest");

        bindingResult.addError(new FieldError("deliveryAssignRequest", "restaurantLocation",
                "Restaurant location is required"));
        bindingResult.addError(new FieldError("deliveryAssignRequest", "customerLocation",
                "Customer location is required"));

        // MethodParameter can be null for our handler usage; it's not used by handler logic.
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException((MethodParameter) null, bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getPath()).isEqualTo("/api/delivery/estimate");

        assertThat(response.getBody().getDetails()).isNotNull();
        assertThat(response.getBody().getDetails()).containsKey("fieldErrors");

        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors =
                (Map<String, String>) response.getBody().getDetails().get("fieldErrors");

        assertThat(fieldErrors).containsEntry("restaurantLocation", "Restaurant location is required");
        assertThat(fieldErrors).containsEntry("customerLocation", "Customer location is required");
    }

    @Test
    void handleBadJson_returns400() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("bad json");

        ResponseEntity<ApiError> response = handler.handleBadJson(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Malformed JSON request");
        assertThat(response.getBody().getPath()).isEqualTo("/api/delivery/estimate");
    }

    @Test
    void handleBadRequest_returns400_withMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid status");

        ResponseEntity<ApiError> response = handler.handleBadRequest(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid status");
        assertThat(response.getBody().getPath()).isEqualTo("/api/delivery/estimate");
    }

    @Test
    void handleGeneric_returns500_includesReasonInDetails() {
        Exception ex = new RuntimeException("Something failed");

        ResponseEntity<ApiError> response = handler.handleGeneric(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
        assertThat(response.getBody().getPath()).isEqualTo("/api/delivery/estimate");

        assertThat(response.getBody().getDetails()).isNotNull();
        assertThat(response.getBody().getDetails()).containsEntry("reason", "Something failed");
    }


    @Test
    void handleGeneric_returns500() {
        Exception ex = new RuntimeException("Something failed");

        ResponseEntity<ApiError> response = handler.handleGeneric(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
    }
}
