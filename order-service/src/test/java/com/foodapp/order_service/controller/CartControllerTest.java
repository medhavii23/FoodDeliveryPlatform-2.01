package com.foodapp.order_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodapp.order_service.constants.Constants;
import com.foodapp.order_service.dto.AddToCartResponse;
import com.foodapp.order_service.dto.CartItemUpdateRequest;
import com.foodapp.order_service.dto.CartViewResponse;
import com.foodapp.order_service.dto.OrderSyncRequest;
import com.foodapp.order_service.exception.ApiError;
import com.foodapp.order_service.model.CartStatus;
import com.foodapp.order_service.model.Order;
import com.foodapp.order_service.model.OrderStatus;
import com.foodapp.order_service.service.CartService;
import com.foodapp.order_service.service.CustomerService;
import com.foodapp.order_service.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private final UUID CUSTOMER_ID = UUID.randomUUID();
    private final UUID ORDER_ID = UUID.randomUUID();

    // ---------- ADD / UPDATE ITEM ----------

    @Test
    void addItem_success() throws Exception {
        CartItemUpdateRequest request = new CartItemUpdateRequest();
        request.setRestaurantName("Rest");
        request.setDeliveryArea("Area");
        request.setItemName("Item");
        request.setQty(1);

        when(cartService.addOrUpdateItem(any(), any(), any()))
                .thenReturn(new AddToCartResponse());

        mockMvc.perform(post("/api/cart/items")
                        .header(Constants.AUTH_ID, CUSTOMER_ID)
                        .header(Constants.AUTH_USER, "User")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }


    @Test
    void addItem_missingAuthId_shouldFail() throws Exception {
        CartItemUpdateRequest request = new CartItemUpdateRequest();
        request.setRestaurantName("Rest");
        request.setDeliveryArea("Area");
        request.setItemName("Item");
        request.setQty(1);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---------- VIEW CART ----------

    @Test
    void getCart_success() throws Exception {
        when(cartService.getActiveCart(any(), any()))
                .thenReturn(new CartViewResponse());

        mockMvc.perform(get("/api/cart/my")
                        .header(Constants.AUTH_ID, CUSTOMER_ID))
                .andExpect(status().isOk());
    }

    @Test
    void getCart_missingAuthId_shouldFail() throws Exception {
        mockMvc.perform(get("/api/cart/my"))
                .andExpect(status().isBadRequest());
    }

    // ---------- CLEAR CART ----------

    @Test
    void clearCart_success() throws Exception {
        mockMvc.perform(delete("/api/cart/my")
                        .header(Constants.AUTH_ID, CUSTOMER_ID))
                .andExpect(status().isNoContent());
    }

    // ---------- CHECKOUT ----------

    @Test
    void checkout_success() throws Exception {
        when(cartService.checkout(any(), any(), any()))
                .thenReturn(new Order());

        mockMvc.perform(post("/api/cart/checkout")
                        .header(Constants.AUTH_ID, CUSTOMER_ID)
                        .header(Constants.CUSTOMER_NAME, "User"))
                .andExpect(status().isCreated());
    }

    @Test
    void checkout_missingAuthId_shouldFail() throws Exception {
        mockMvc.perform(post("/api/cart/checkout"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void checkout_authUserNull_usesCustomerName() throws Exception {
        when(cartService.checkout(any(), any(), any()))
                .thenReturn(new Order());

        mockMvc.perform(post("/api/cart/checkout")
                        .header(Constants.AUTH_ID, CUSTOMER_ID)
                        .header(Constants.CUSTOMER_NAME, "FallbackUser"))
                .andExpect(status().isCreated());
    }


    // ---------- MY ORDERS ----------

    @Test
    void myOrders_success_withVerification() throws Exception {
        mockMvc.perform(get("/api/cart/orders")
                        .header(Constants.AUTH_ID, CUSTOMER_ID)
                        .header(Constants.AUTH_USER, "User"))
                .andExpect(status().isOk());
    }

    @Test
    void myOrders_withUserAndCustomerId_triggersVerification() throws Exception {
        mockMvc.perform(get("/api/cart/orders")
                        .header(Constants.AUTH_ID, CUSTOMER_ID)
                        .header(Constants.AUTH_USER, "User"))
                .andExpect(status().isOk());
    }

    @Test
    void myOrders_withoutUserName_shouldSkipVerification() throws Exception {
        mockMvc.perform(get("/api/cart/orders")
                        .header(Constants.AUTH_ID, CUSTOMER_ID))
                .andExpect(status().isOk());
    }

    // ---------- SINGLE ORDER ----------

    @Test
    void myOrder_success() throws Exception {
        when(orderService.getUserOrder(any(), any()))
                .thenReturn(new Order());

        mockMvc.perform(get("/api/cart/orders/{id}", ORDER_ID)
                        .header(Constants.AUTH_ID, CUSTOMER_ID)
                        .header(Constants.CUSTOMER_NAME, "User"))
                .andExpect(status().isOk());
    }
    @Test
    void myOrder_withAuthUser_triggersVerification() throws Exception {
        when(orderService.getUserOrder(any(), any()))
                .thenReturn(new Order());

        mockMvc.perform(get("/api/cart/orders/{id}", ORDER_ID)
                        .header(Constants.AUTH_ID, CUSTOMER_ID)
                        .header(Constants.AUTH_USER, "User"))
                .andExpect(status().isOk());
    }


    // ---------- ORDER SUMMARY ----------

    @Test
    void orderSummary_success() throws Exception {
        when(orderService.getCustomerOrderSummary(any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/cart/orders/summary")
                        .header(Constants.AUTH_ID, CUSTOMER_ID))
                .andExpect(status().isOk());
    }

    @Test
    void orderSummary_missingAuthId_shouldFail() throws Exception {
        mockMvc.perform(get("/api/cart/orders/summary"))
                .andExpect(status().isBadRequest());
    }

    // ---------- ORDER SYNC ----------

    @Test
    void syncOrder_success() throws Exception {
        OrderSyncRequest req = new OrderSyncRequest();
        req.setOrderId(ORDER_ID);

        when(orderService.syncOrder(any()))
                .thenReturn(new Order());

        mockMvc.perform(post("/api/cart/orders/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted());
    }

    // ---------- UPDATE STATUS (ADMIN) ----------

    @Test
    void updateStatus_admin_success() throws Exception {
        when(orderService.updateOrderStatus(any(), any()))
                .thenReturn(new Order());

        mockMvc.perform(put("/api/cart/orders/{id}/status", ORDER_ID)
                        .header(Constants.AUTH_ROLE, Constants.ROLE_ADMIN)
                        .param("status", OrderStatus.PLACED.name()))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_nonAdmin_forbidden() throws Exception {

        mockMvc.perform(put("/api/cart/orders/{id}/status", ORDER_ID)
                        .header(Constants.AUTH_ROLE, "USER")
                        .param("status", OrderStatus.PLACED.name()))
                .andExpect(status().isForbidden());
    }





}
