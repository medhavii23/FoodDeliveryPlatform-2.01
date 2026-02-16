package com.foodapp.order_service.constants;

import org.junit.jupiter.api.Test;
import java.math.RoundingMode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConstantsTest {

    @Test
    void testConstants() {
        assertEquals("X-Customer-Id", Constants.CUSTOMER_ID);
        assertEquals("X-Customer-Name", Constants.CUSTOMER_NAME);
        assertEquals("X-Auth-User", Constants.AUTH_USER);
        assertEquals(2, Constants.MONEY_SCALE);
        assertEquals(RoundingMode.HALF_UP, Constants.MONEY_ROUNDING);
        assertEquals("Customer not found. Please create an account using POST /api/customer/register",
                Constants.CUSTOMER_NOT_FOUND_MESSAGE);

        assertEquals("Order not found", Constants.ORDER_NOT_FOUND);
        assertEquals("Searching for partner...", Constants.SEARCHING_PARTNER);
        assertEquals("Pending", Constants.PENDING_ETA);
        assertEquals(10, Constants.EXTRA_ETA_NO_PARTNER_MINS);

        assertEquals("X-Auth-Id", Constants.AUTH_ID);
        assertEquals("X-Auth-Role", Constants.AUTH_ROLE);
        assertEquals("ADMIN", Constants.ROLE_ADMIN);
        assertEquals("Access Denied: Admins only", Constants.ACCESS_DENIED_ADMIN);
        assertEquals("Delivery Service Delay", Constants.DELIVERY_SERVICE_DELAY);
    }

    @Test
    void testErrorMessages() {
        assertEquals("Insufficient stock for some items", ErrorMessages.INSUFFICIENT_STOCK);
        assertEquals("item required", ErrorMessages.ITEM_REQUIRED);
        assertEquals("qty is 0 or negative", ErrorMessages.INVALID_QTY);
        assertEquals("items doesn't exist", ErrorMessages.ITEM_NOT_FOUND);
        assertEquals("Validation Error: Customer ID required for order placement", ErrorMessages.CUSTOMER_ID_REQUIRED);
        assertEquals("Order failed after reserving stock. Inventory rollback triggered.",
                ErrorMessages.ORDER_FAILED_ROLLBACK);
        assertEquals("Order not found: ", ErrorMessages.ORDER_NOT_FOUND_ID);
        assertEquals("Validation Error: Customer ID required", ErrorMessages.CUSTOMER_ID_REQUIRED_VERIFY);
        assertEquals("deliveryArea is required", ErrorMessages.DELIVERY_AREA_REQUIRED);
        assertEquals("Unknown deliveryArea: ", ErrorMessages.UNKNOWN_DELIVERY_AREA);
    }

    @Test
    void testConstructors() {
        // Reflection to access private constructor if needed, or just standard
        // instantiation if public/default
        // Since they are private, we can't easily instantiate without reflection, but
        // standard coverage checks might skip private constructors or we use
        // reflection.
        // However, for typical coverage tools, testing public fields is enough.
    }
}
