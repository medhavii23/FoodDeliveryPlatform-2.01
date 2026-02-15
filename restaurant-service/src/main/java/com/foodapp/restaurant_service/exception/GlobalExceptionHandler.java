package com.foodapp.restaurant_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global REST exception handler for restaurant service; maps exceptions to {@link ApiError} responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RestaurantNotFoundException.class)
    public ResponseEntity<ApiError> handleRestaurantNotFound(RestaurantNotFoundException ex, HttpServletRequest req) {
        log.debug("Restaurant not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(MenuItemNotFoundException.class)
    public ResponseEntity<ApiError> handleMenuItemNotFound(MenuItemNotFoundException ex, HttpServletRequest req) {
        log.debug("MenuItem not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, Object> details = new LinkedHashMap<>();
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));
        details.put("fieldErrors", fieldErrors);
        log.debug("Validation failed: {}", ex.getBindingResult().getFieldErrors());
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req.getRequestURI(), details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.warn("Malformed JSON: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request", req.getRequestURI(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        log.debug("Bad request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req.getRequestURI(),
                Map.of("reason", ex.getMessage()));
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, String path, Map<String, Object> details) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, details);
        return ResponseEntity.status(status).body(body);
    }
}
