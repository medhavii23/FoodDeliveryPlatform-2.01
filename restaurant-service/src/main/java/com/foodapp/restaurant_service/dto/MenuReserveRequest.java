package com.foodapp.restaurant_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class  MenuReserveRequest {
    @NotBlank private String restaurantName;
    @NotEmpty @Valid
    private List<ItemNameQty> items;
}
