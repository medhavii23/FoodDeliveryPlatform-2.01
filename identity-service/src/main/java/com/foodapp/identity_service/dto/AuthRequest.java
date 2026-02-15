package com.foodapp.identity_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {
    @jakarta.validation.constraints.NotBlank(message = "Username is required")
    private String username;

    @jakarta.validation.constraints.NotBlank(message = "Password is required")
    private String password;
}
