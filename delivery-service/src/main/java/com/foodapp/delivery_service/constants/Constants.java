package com.foodapp.delivery_service.constants;

import java.math.RoundingMode;

public class Constants {
    private Constants() {
    }

    public static final int MONEY_SCALE = 2;
    public static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    public static final double EARTH_RADIUS = 6371.0;
    public static final int FREE_DELIVERY_DISTANCE = 2;
    public static final int CHARGE_PER_KM = 10;
    public static final int TIME_PER_KM = 5;

    public static final String COORDINATES_INVALID = "Invalid coordinates for ";
    public static final String COORDINATES_MISSING = "Coordinates missing for ";

    public static final String NA = "N/A";
    public static final String ASSIGN_SUCCESS = "Partner assigned successfully";
    public static final String ASSIGN_FAIL_NO_PARTNER = "No delivery partner available at the moment";

    public static final String AUTH_ROLE = "X-Auth-Role";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ACCESS_DENIED_ADMIN = "Access Denied: Admins only";
    public static final String ACCESS_DENIED_MISSING_ROLE = "Access Denied: Missing role header";

    public static final String LOCATION_REQUIRED = "Location name is required";
    public static final String LOCATION_UNKNOWN = "Unknown location: ";
}
