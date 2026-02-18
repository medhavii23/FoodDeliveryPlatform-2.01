package com.foodapp.restaurant_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/test/path");
    }

    @Test
    void testHandleRestaurantNotFound() {
        RestaurantNotFoundException ex =
                new RestaurantNotFoundException("Not found");

        ResponseEntity<ApiError> response =
                handler.handleRestaurantNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not found", response.getBody().getMessage());
    }

    @Test
    void testHandleMenuItemNotFound() {
        MenuItemNotFoundException ex =
                new MenuItemNotFoundException("Item missing");

        ResponseEntity<ApiError> response =
                handler.handleMenuItemNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Item missing", response.getBody().getMessage());
    }

    @Test
    void testHandleValidation() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "target");

        bindingResult.addError(new FieldError(
                "target",
                "restaurantName",
                "must not be blank"));

        Method method = Object.class.getMethods()[0];

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(
                        new org.springframework.core.MethodParameter(method, -1),
                        bindingResult
                );

        ResponseEntity<ApiError> response =
                handler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getDetails());
    }

    @Test
    void testHandleBadJson() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("Bad JSON");

        ResponseEntity<ApiError> response =
                handler.handleBadJson(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed JSON request", response.getBody().getMessage());
    }

    @Test
    void testHandleBadRequest() {
        IllegalArgumentException ex =
                new IllegalArgumentException("Invalid input");

        ResponseEntity<ApiError> response =
                handler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid input", response.getBody().getMessage());
    }

    @Test
    void testHandleGeneric() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<ApiError> response =
                handler.handleGeneric(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal server error", response.getBody().getMessage());
        assertNotNull(response.getBody().getDetails());
    }
}
