package com.foodapp.order_service.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class OrderModelsTest {

    @Test
    void testOrder() {
        Order order = new Order();
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Instant now = Instant.now();

        order.setOrderId(orderId);
        order.setCustomerId(customerId);
        order.setRestaurantId(1L);
        order.setFoodAmount(BigDecimal.TEN);
        order.setTotalAmount(BigDecimal.valueOf(20));
        order.setStatus(OrderStatus.PLACED);
        order.setCreatedAt(now);

        assertEquals(orderId, order.getOrderId());
        assertEquals(customerId, order.getCustomerId());
        assertEquals(1L, order.getRestaurantId());
        assertEquals(BigDecimal.TEN, order.getFoodAmount());
        assertEquals(BigDecimal.valueOf(20), order.getTotalAmount());
        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals(now, order.getCreatedAt());

        Order order2 = new Order();
        assertNull(order2.getOrderId());
    }

    @Test
    void testCart() {
        Cart cart = new Cart();
        UUID cartId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        cart.setCartId(cartId);
        cart.setCustomerId(customerId);
        cart.setRestaurantId(10L);
        cart.setDeliveryArea("North");
        cart.setStatus(CartStatus.ACTIVE);

        assertEquals(cartId, cart.getCartId());
        assertEquals(customerId, cart.getCustomerId());
        assertEquals(10L, cart.getRestaurantId());
        assertEquals("North", cart.getDeliveryArea());
        assertEquals(CartStatus.ACTIVE, cart.getStatus());
    }

    @Test
    void testCartItem() {
        CartItem item = new CartItem();
        item.setItemName("Burger");
        item.setQty(2);

        assertEquals("Burger", item.getItemName());
        assertEquals(2, item.getQty());
    }

    @Test
    void testCustomer() {
        Customer c = new Customer();
        UUID id = UUID.randomUUID();
        c.setCustomerId(id);

        assertEquals(id, c.getCustomerId());
    }

    @Test
    void testOrderStatus() {
        assertEquals(OrderStatus.PLACED, OrderStatus.valueOf("PLACED"));
        assertEquals(OrderStatus.PREPARING, OrderStatus.valueOf("PREPARING"));
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.valueOf("OUT_FOR_DELIVERY"));
        assertEquals(OrderStatus.DELIVERED, OrderStatus.valueOf("DELIVERED"));
    }
}
