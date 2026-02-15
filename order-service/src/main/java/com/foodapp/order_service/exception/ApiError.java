package com.foodapp.order_service.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, Object> details;
}
