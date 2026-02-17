package com.foodapp.order_service.service;

import com.foodapp.order_service.dto.*;
import com.foodapp.order_service.feign.DeliveryClient;
import com.foodapp.order_service.feign.RestaurantClient;
import com.foodapp.order_service.model.Cart;
import com.foodapp.order_service.model.CartItem;
import com.foodapp.order_service.model.CartStatus;
import com.foodapp.order_service.model.Order;
import com.foodapp.order_service.repository.CartItemRepository;
import com.foodapp.order_service.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock CustomerService customerService;
    @Mock OrderService orderService;
    @Mock RestaurantClient restaurantClient;
    @Mock DeliveryClient deliveryClient;

    @InjectMocks
    CartService cartService;

    private UUID customerId;
    private Cart cart;

    @BeforeEach
    void setup() {
        customerId = UUID.randomUUID();
        cart = new Cart();
        cart.setCartId(UUID.randomUUID());
        cart.setCustomerId(customerId);
        cart.setRestaurantId(1L);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setDeliveryArea("Area1");
        cart.setCreatedAt(Instant.now());
        cart.setUpdatedAt(Instant.now());
    }

    /* ---------------- ADD / UPDATE ITEM ---------------- */

    @Test
    void addItem_existingCart_existingItem() {
        CartItemUpdateRequest req = new CartItemUpdateRequest();
        req.setRestaurantName("Rest");
        req.setItemName("Pizza");
        req.setQty(2);
        req.setDeliveryArea("Area1");

        CartItem item = new CartItem();
        item.setItemName("Pizza");

        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantId(1L);
        res.setRestaurantName("Rest");

        when(restaurantClient.getRestaurantByName("Rest")).thenReturn(res);
        when(cartRepository.findByCustomerIdAndRestaurantIdAndStatus(customerId, 1L, CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartIdAndItemNameIgnoreCase(cart.getCartId(), "Pizza"))
                .thenReturn(Optional.of(item));
        when(cartItemRepository.findByCart_CartId(any())).thenReturn(List.of(item));
        when(restaurantClient.getRestaurantById(1L)).thenReturn(res);
        when(cartRepository.save(any())).thenReturn(cart);

        cartService.addOrUpdateItem(customerId, "User", req);

        verify(cartItemRepository).save(item);
    }

    @Test
    void removeItem_qtyZero() {
        CartItemUpdateRequest req = new CartItemUpdateRequest();
        req.setRestaurantName("Rest");
        req.setItemName("Pizza");
        req.setQty(0);

        CartItem item = new CartItem();

        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantId(1L);

        when(restaurantClient.getRestaurantByName("Rest")).thenReturn(res);
        when(cartRepository.findByCustomerIdAndRestaurantIdAndStatus(any(), any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartIdAndItemNameIgnoreCase(any(), any()))
                .thenReturn(Optional.of(item));

        cartService.addOrUpdateItem(customerId, "User", req);

        verify(cartItemRepository).delete(item);
    }

    /* ---------------- GET ACTIVE CART ---------------- */

    @Test
    void getActiveCart_noCart() {
        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.empty());

        assertNull(cartService.getActiveCart(customerId, null));
    }

    @Test
    void getActiveCart_restaurantException() {
        when(restaurantClient.getRestaurantByName(any()))
                .thenThrow(new RuntimeException());

        assertNull(cartService.getActiveCart(customerId, "X"));
    }

    /* ---------------- CLEAR CART ---------------- */

    @Test
    void clearCart_noActiveCart() {
        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.empty());

        cartService.clearActiveCart(customerId, null);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void clearCart_success() {
        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));

        cartService.clearActiveCart(customerId, null);

        assertEquals(CartStatus.CANCELLED, cart.getStatus());
        verify(cartItemRepository).deleteByCart_CartId(cart.getCartId());
    }

    /* ---------------- CHECKOUT ---------------- */

    @Test
    void checkout_noCart() {
        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> cartService.checkout(customerId, "User", null));
    }

    @Test
    void checkout_emptyCart() {
        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of());

        assertThrows(IllegalStateException.class,
                () -> cartService.checkout(customerId, "User", null));
    }

    @Test
    void checkout_withRestaurantOverride_exception() {
        when(restaurantClient.getRestaurantByName(any()))
                .thenThrow(new RuntimeException());

        assertThrows(IllegalStateException.class,
                () -> cartService.checkout(customerId, "User", "X"));
    }

    /* ---------------- DELIVERY ESTIMATION ---------------- */

    @Test
    void viewCart_deliverySuccess() {
        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantName("Rest");
        res.setLocationName("Loc");

        DeliveryResponse deliveryResp = new DeliveryResponse();
        deliveryResp.setSuccess(true);
        deliveryResp.setDeliveryCharge(BigDecimal.TEN);
        deliveryResp.setEta("30 mins");

        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(new CartItem()));
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(res);
        when(deliveryClient.estimateDelivery(any()))
                .thenReturn(deliveryResp);

        CartViewResponse response = cartService.getActiveCart(customerId, null);

        assertEquals(BigDecimal.TEN.setScale(2), response.getDeliveryCharge());
    }

    @Test
    void viewCart_deliveryException() {
        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(new CartItem()));
        when(restaurantClient.getRestaurantById(any()))
                .thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> {
                cartService.getActiveCart(customerId, null);
            });

    }
}
