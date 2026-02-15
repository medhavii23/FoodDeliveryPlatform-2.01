package com.foodapp.restaurant_service.exception;

public class MenuItemNotFoundException extends RuntimeException {
    public MenuItemNotFoundException(String message) {
        super(message);
    }
}
