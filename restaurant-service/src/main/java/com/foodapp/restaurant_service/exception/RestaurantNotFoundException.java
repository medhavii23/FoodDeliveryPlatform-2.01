package com.foodapp.restaurant_service.exception;

public class RestaurantNotFoundException extends RuntimeException {
    public RestaurantNotFoundException(String message) {
        super(message);
    }

    public static RestaurantNotFoundException byId(Long id) {
        return new RestaurantNotFoundException("Restaurant not found: " + id);
    }
}
