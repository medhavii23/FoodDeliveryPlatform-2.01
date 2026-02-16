package com.foodapp.order_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodapp.order_service.constants.Constants;
import com.foodapp.order_service.dto.AddToCartResponse;
import com.foodapp.order_service.dto.CartItemUpdateRequest;
import com.foodapp.order_service.dto.CartViewResponse;
import com.foodapp.order_service.model.CartStatus;
import com.foodapp.order_service.model.Order;
import com.foodapp.order_service.service.CartService;
import com.foodapp.order_service.service.CustomerService;
import com.foodapp.order_service.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testAddOrUpdateItem() throws Exception {
        CartItemUpdateRequest request = new CartItemUpdateRequest();
        request.setRestaurantName("Rest 1");
        request.setItemName("Item 1");
        request.setQty(1);

        AddToCartResponse response = new AddToCartResponse(UUID.randomUUID(), UUID.randomUUID(), "User", "Rest 1",
                "Area 1", CartStatus.ACTIVE, null, null, null, BigDecimal.TEN, BigDecimal.TEN);

        when(cartService.addOrUpdateItem(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/cart/items")
                .header(Constants.AUTH_ID, UUID.randomUUID())
                .header(Constants.AUTH_USER, "User")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testGetActiveCart() throws Exception {
        CartViewResponse response = new CartViewResponse();
        when(cartService.getActiveCart(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/cart/my")
                .header(Constants.AUTH_ID, UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void testCheckout() throws Exception {
        Order order = new Order();
        when(cartService.checkout(any(), any(), any())).thenReturn(order);

        mockMvc.perform(post("/api/cart/checkout")
                .header(Constants.AUTH_ID, UUID.randomUUID()))
                .andExpect(status().isCreated());
    }

    @Test
    void testMyOrders() throws Exception {
        mockMvc.perform(get("/api/cart/orders")
                .header(Constants.AUTH_ID, UUID.randomUUID()))
                .andExpect(status().isOk());
    }
}
