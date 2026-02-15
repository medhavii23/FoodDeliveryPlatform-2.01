package com.foodapp.order_service.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REASON = "reason";

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiError> handleCustomerNotFound(CustomerNotFoundException ex, HttpServletRequest req){
        return build(HttpStatus.NOT_FOUND, ex.getMessage(),req.getRequestURI(),null);
    }

    @ExceptionHandler(InvalidCustomerCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCustomer(InvalidCustomerCredentialsException ex, HttpServletRequest req){
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(),req.getRequestURI(),null);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> handleInsufficientStock(InsufficientStockException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI(), Map.of("items", ex.getFailures()));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiError> handleOrderNotFound(OrderNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(OrderProcessingException.class)
    public ResponseEntity<ApiError> handleOrderProcessing(OrderProcessingException ex, HttpServletRequest req) {
        Throwable cause = ex.getCause();

        if (cause instanceof FeignException fe) {
            return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), req.getRequestURI(),
                    Map.of("downstreamStatus", fe.status(), REASON, fe.getMessage()));
        }

        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), req.getRequestURI(),
                Map.of(REASON, cause == null ? null : cause.getMessage()));
    }


    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiError> handleFeign(FeignException ex, HttpServletRequest req) {
        HttpStatus status;

        if (ex.status() == -1) status = HttpStatus.SERVICE_UNAVAILABLE; // cannot reach service
        else if (ex.status() >= 500) status = HttpStatus.BAD_GATEWAY;   // downstream 5xx
        else status = HttpStatus.BAD_GATEWAY; // downstream 4xx - still dependency failure in your flow

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("downstreamStatus", ex.status());
        return build(status, "Downstream service call failed", req.getRequestURI(), details);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, Object> details = new LinkedHashMap<>();
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));
        details.put("fieldErrors", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req.getRequestURI(), details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request", req.getRequestURI(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req.getRequestURI(),
                Map.of(REASON, ex.getMessage()));
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, String path, Map<String, Object> details) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, details);
        return ResponseEntity.status(status).body(body);
    }
}
