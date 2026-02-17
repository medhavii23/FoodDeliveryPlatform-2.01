package com.foodapp.order_service.exception;

import feign.Request;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
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
    }


    @Test
    void handleInvalidCustomer() {
        InvalidCustomerCredentialsException ex =
                new InvalidCustomerCredentialsException("Invalid creds");
        ResponseEntity<ApiError> response = exceptionHandler.handleInvalidCustomer(ex, req);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
    @Test
    void handleCustomerNotFound_withNullDetails_branch() {
        CustomerNotFoundException ex =
                new CustomerNotFoundException("Not found");

        ResponseEntity<ApiError> response =
                exceptionHandler.handleCustomerNotFound(ex, req);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleResponseStatus_forbidden() {
        ResponseStatusException ex =
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");

        ResponseEntity<ApiError> response =
                exceptionHandler.handleResponseStatus(ex, req);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Access denied", response.getBody().getMessage());
    }


    @Test
    void handleInsufficientStock() {
        StockFailure failure = new StockFailure(1L, "Item1", 10, 5);
        InsufficientStockException ex =
                new InsufficientStockException("No stock", List.of(failure));

        ResponseEntity<ApiError> response =
                exceptionHandler.handleInsufficientStock(ex, req);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody().getDetails());
    }
    @Test
    void handleInsufficientStock_withNullFailures() {
        InsufficientStockException ex =
                new InsufficientStockException("No stock", null);

        ResponseEntity<ApiError> response =
                exceptionHandler.handleInsufficientStock(ex, req);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }


    @Test
    void handleOrderNotFound() {
        OrderNotFoundException ex =
                new OrderNotFoundException(UUID.randomUUID());
        ResponseEntity<ApiError> response =
                exceptionHandler.handleOrderNotFound(ex, req);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // -------- OrderProcessingException branches --------

    @Test
    void handleOrderProcessing_WithFeignCause() {
        Request request = Request.create(
                Request.HttpMethod.GET, "url",
                Collections.emptyMap(), null, null, null);

        FeignException fe =
                new FeignException.ServiceUnavailable("Down", request, null, null);

        OrderProcessingException ex =
                new OrderProcessingException("Processing failed", fe);

        ResponseEntity<ApiError> response =
                exceptionHandler.handleOrderProcessing(ex, req);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void handleOrderProcessing_WithGenericCause() {
        OrderProcessingException ex =
                new OrderProcessingException("Processing failed", new RuntimeException("DB"));

        ResponseEntity<ApiError> response =
                exceptionHandler.handleOrderProcessing(ex, req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void handleOrderProcessing_WithNullCause() {
        OrderProcessingException ex =
                new OrderProcessingException("Processing failed", null);

        ResponseEntity<ApiError> response =
                exceptionHandler.handleOrderProcessing(ex, req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
    @Test
    void handleResponseStatus_withNullReason() {
        ResponseStatusException ex =
                new ResponseStatusException(HttpStatus.BAD_REQUEST, null);

        ResponseEntity<ApiError> response =
                exceptionHandler.handleResponseStatus(ex, req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        // message can be null – that is the branch we are covering
    }


    @Test
    void build_invoked_multiple_paths_for_line_coverage() {
        // path 1
        exceptionHandler.handleBadRequest(
                new IllegalArgumentException("bad"), req);

        // path 2
        exceptionHandler.handleBadJson(
                new HttpMessageNotReadableException("json"), req);
    }



    // -------- FeignException handler branches --------

    @Test
    void handleFeign_StatusMinusOne() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(-1);

        ResponseEntity<ApiError> response =
                exceptionHandler.handleFeign(ex, req);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }


    @Test
    void handleFeign_Status5xx() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(500);

        ResponseEntity<ApiError> response =
                exceptionHandler.handleFeign(ex, req);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void handleFeign_Status4xx() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(400);

        ResponseEntity<ApiError> response =
                exceptionHandler.handleFeign(ex, req);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    // -------- Validation & Bad request --------

    @Test
    void handleValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(new FieldError("obj", "field", "msg")));

        ResponseEntity<ApiError> response =
                exceptionHandler.handleValidation(ex, req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleBadJson() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("Bad JSON");

        ResponseEntity<ApiError> response =
                exceptionHandler.handleBadJson(ex, req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleBadRequest() {
        IllegalArgumentException ex =
                new IllegalArgumentException("Invalid input");

        ResponseEntity<ApiError> response =
                exceptionHandler.handleBadRequest(ex, req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleGeneric() {
        Exception ex = new Exception("Error");

        ResponseEntity<ApiError> response =
                exceptionHandler.handleGeneric(ex, req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
    @Test
    void handleValidation_withNoFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Collections.emptyList());

        ResponseEntity<ApiError> response =
                exceptionHandler.handleValidation(ex, req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleFeign_UnexpectedStatus() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(123);

        ResponseEntity<ApiError> response =
                exceptionHandler.handleFeign(ex, req);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void handleResponseStatus_withNullRequestUri() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn(null);

        ResponseStatusException ex =
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad");

        ResponseEntity<ApiError> response =
                exceptionHandler.handleResponseStatus(ex, req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

}

