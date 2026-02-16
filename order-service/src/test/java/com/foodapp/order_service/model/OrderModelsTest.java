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
        cart.setCartId("cart1");
        cart.setRestaurantId(10L);
        cart.setTotalPrice(BigDecimal.valueOf(100));

        assertEquals("cart1", cart.getCartId());
        assertEquals(10L, cart.getRestaurantId());
        assertEquals(BigDecimal.valueOf(100), cart.getTotalPrice());
    }

    @Test
    void testCartItem() {
        CartItem item = new CartItem();
        item.setItemId(1L);
        item.setName("Burger");
        item.setPrice(BigDecimal.valueOf(10));
        item.setQuantity(2);

        assertEquals(1L, item.getItemId());
        assertEquals("Burger", item.getName());
        assertEquals(BigDecimal.valueOf(10), item.getPrice());
        assertEquals(2, item.getQuantity());
    }

    @Test
    void testCustomer() {
        Customer c = new Customer();
        UUID id = UUID.randomUUID();
        c.setId(id);
        c.setName("John");
        c.setEmail("john@example.com");

        assertEquals(id, c.getId());
        assertEquals("John", c.getName());
        assertEquals("john@example.com", c.getEmail());
    }
}
