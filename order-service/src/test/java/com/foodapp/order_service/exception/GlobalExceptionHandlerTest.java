package com.foodapp.order_service.exception;

import feign.Request;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest req;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/test/uri");
    }

    @Test
    void handleCustomerNotFound() {
        CustomerNotFoundException ex = new CustomerNotFoundException("Customer not found");
        ResponseEntity<ApiError> response = exceptionHandler.handleCustomerNotFound(ex, req);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Customer not found", response.getBody().getMessage());
    }

    @Test
    void handleInvalidCustomer() {
        InvalidCustomerCredentialsException ex = new InvalidCustomerCredentialsException("Invalid creds");
        ResponseEntity<ApiError> response = exceptionHandler.handleInvalidCustomer(ex, req);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void handleInsufficientStock() {
        StockFailure failure = new StockFailure(1L, "Item1", 10, 5);
        InsufficientStockException ex = new InsufficientStockException("No stock", List.of(failure));
        ResponseEntity<ApiError> response = exceptionHandler.handleInsufficientStock(ex, req);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("No stock", response.getBody().getMessage());
    }

    @Test
    void handleOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        OrderNotFoundException ex = new OrderNotFoundException(orderId);
        ResponseEntity<ApiError> response = exceptionHandler.handleOrderNotFound(ex, req);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void handleOrderProcessing_FeignCause() {
        Request request = Request.create(Request.HttpMethod.GET, "url", Collections.emptyMap(), null, null, null);
        FeignException fe = new FeignException.ServiceUnavailable("Service Down", request, null, null);
        OrderProcessingException ex = new OrderProcessingException("Processing failed", fe);

        ResponseEntity<ApiError> response = exceptionHandler.handleOrderProcessing(ex, req);
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void handleOrderProcessing_GenericCause() {
        Exception cause = new Exception("Database error");
        OrderProcessingException ex = new OrderProcessingException("Processing failed", cause);

        ResponseEntity<ApiError> response = exceptionHandler.handleOrderProcessing(ex, req);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void handleValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        FieldError fieldError = new FieldError("obj", "field", "errorMsg");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiError> response = exceptionHandler.handleValidation(ex, req);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleGeneric() {
        Exception ex = new Exception("Error");
        ResponseEntity<ApiError> response = exceptionHandler.handleGeneric(ex, req);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
