package com.foodapp.restaurant_service.constants;


public class Constants {
    private Constants() {

    }

    public static final String ITEM_ALREADY_EXISTS = "Menu Item already exists in this restaurant: ";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ACCESS_DENIED_ADMIN = "Access Denied: Admins only";
    public static final String ACCESS_DENIED_MISSING_ROLE = "Access Denied: Missing role header";
}
