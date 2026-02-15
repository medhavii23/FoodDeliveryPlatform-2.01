package com.foodapp.order_service.constants;

import java.math.RoundingMode;

public class Constants {
    private Constants() {

    }

    public static final String CUSTOMER_ID = "X-Customer-Id";
    public static final String CUSTOMER_NAME = "X-Customer-Name";
    public static final String AUTH_USER = "X-Auth-User";
    public static final int MONEY_SCALE = 2;
    public static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
    public static final String CUSTOMER_NOT_FOUND_MESSAGE = "Customer not found. Please create an account using POST /api/customer/register";

    public static final String ORDER_NOT_FOUND = "Order not found";
    public static final String SEARCHING_PARTNER = "Searching for partner...";
    public static final String PENDING_ETA = "Pending";
    public static final int EXTRA_ETA_NO_PARTNER_MINS = 10;

    public static final String AUTH_ID = "X-Auth-Id";
    public static final String AUTH_ROLE = "X-Auth-Role";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ACCESS_DENIED_ADMIN = "Access Denied: Admins only";
    public static final String DELIVERY_SERVICE_DELAY = "Delivery Service Delay";
}
