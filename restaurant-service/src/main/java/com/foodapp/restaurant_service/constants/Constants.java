package com.foodapp.restaurant_service.constants;

import java.math.RoundingMode;

public class Constants {
    private Constants() {

    }

    public static final int MONEY_SCALE = 2;
    public static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    // Add specific restaurant constants here
    public static final String RESTAURANT_NOT_FOUND = "Restaurant not found";
    public static final String RESTAURANT_CLOSED = "Restaurant is closed";
    public static final String ITEM_MISSING = "ItemId missing";
    public static final String ITEM_NOT_FOUND = "Item not found for itemId=";
    public static final String ITEM_UNAVAILABLE = "Item currently unavailable: ";
    public static final String INVALID_QTY = "Invalid quantity for itemId=";
    public static final String NAME_MISMATCH = "Item name mismatch for itemId=";
    public static final String NO_ITEMS = "No items provided";
    public static final String ITEM_NOT_AVAILABLE = "Menu item not available: ";
    public static final String VALIDATION_SUCCESS = "OK";
    public static final String ITEM_ALREADY_EXISTS = "Menu Item already exists in this restaurant: ";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ACCESS_DENIED_ADMIN = "Access Denied: Admins only";
    public static final String ACCESS_DENIED_MISSING_ROLE = "Access Denied: Missing role header";
}
