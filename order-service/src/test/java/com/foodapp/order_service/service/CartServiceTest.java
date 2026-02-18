package com.foodapp.order_service.service;

import com.foodapp.order_service.dto.*;
import com.foodapp.order_service.feign.DeliveryClient;
import com.foodapp.order_service.feign.RestaurantClient;
import com.foodapp.order_service.model.*;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    @Test
    void addItem_newCart_created() {
        CartItemUpdateRequest req = new CartItemUpdateRequest();
        req.setRestaurantName("Rest");
        req.setItemName("Burger");
        req.setQty(2);
        req.setDeliveryArea("Area1");

        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantId(1L);
        res.setRestaurantName("Rest");

        when(restaurantClient.getRestaurantByName("Rest")).thenReturn(res);
        when(cartRepository.findByCustomerIdAndRestaurantIdAndStatus(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCart_CartIdAndItemNameIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());
        when(cartItemRepository.findByCart_CartId(any())).thenReturn(List.of());
        when(restaurantClient.getRestaurantById(any())).thenReturn(res);

        cartService.addOrUpdateItem(customerId, "User", req);

        verify(cartRepository, atLeastOnce()).save(any());
    }

    @Test
    void addItem_qtyNull_removes() {
        CartItemUpdateRequest req = new CartItemUpdateRequest();
        req.setRestaurantName("Rest");
        req.setItemName("Pizza");
        req.setQty(null);

        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantId(1L);

        when(restaurantClient.getRestaurantByName("Rest")).thenReturn(res);
        when(cartRepository.findByCustomerIdAndRestaurantIdAndStatus(any(), any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartIdAndItemNameIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());

        cartService.addOrUpdateItem(customerId, "User", req);

        verify(cartItemRepository, never()).save(any());
    }
    @Test
    void getActiveCart_withRestaurant_success() {
        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantId(1L);
        res.setRestaurantName("Rest");

        when(restaurantClient.getRestaurantByName("Rest")).thenReturn(res);
        when(cartRepository.findByCustomerIdAndRestaurantIdAndStatus(any(), any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any())).thenReturn(List.of());
        when(restaurantClient.getRestaurantById(any())).thenReturn(res);

        assertNotNull(cartService.getActiveCart(customerId, "Rest"));
    }

    @Test
    void clearCart_withRestaurantOverride_success() {
        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantId(1L);

        when(restaurantClient.getRestaurantByName("Rest")).thenReturn(res);
        when(cartRepository.findByCustomerIdAndRestaurantIdAndStatus(any(), any(), any()))
                .thenReturn(Optional.of(cart));

        cartService.clearActiveCart(customerId, "Rest");

        verify(cartItemRepository).deleteByCart_CartId(cart.getCartId());
    }

    @Test
    void checkout_success() {
        CartItem ci = new CartItem();
        ci.setItemName("Pizza");
        ci.setQty(2);

        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantName("Rest");

        Order order = new Order();
        order.setOrderId(UUID.randomUUID());

        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(ci));
        when(restaurantClient.getRestaurantById(any())).thenReturn(res);
        when(orderService.placeOrder(any())).thenReturn(order);

        Order result = cartService.checkout(customerId, "User", null);

        assertEquals(order.getOrderId(), result.getOrderId());
        assertEquals(CartStatus.CHECKED_OUT, cart.getStatus());
    }

    @Test
    void viewCart_quoteFailure_branch() {
        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantName("Rest");

        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(new CartItem()));
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(res);
        when(restaurantClient.quote(any()))
                .thenReturn(null);

        CartViewResponse response = cartService.getActiveCart(customerId, null);

        assertEquals(BigDecimal.ZERO.setScale(2), response.getSubtotal());
    }

    @Test
    void viewCart_deliveryNotSuccess_branch() {
        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantName("Rest");
        res.setLocationName("Loc");

        DeliveryResponse deliveryResp = new DeliveryResponse();
        deliveryResp.setSuccess(false);

        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(new CartItem()));
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(res);
        when(deliveryClient.estimateDelivery(any()))
                .thenReturn(deliveryResp);

        CartViewResponse response = cartService.getActiveCart(customerId, null);

        assertEquals(BigDecimal.ZERO.setScale(2), response.getDeliveryCharge());
    }

    @Test
    void normalizeMoney_null_branch() throws Exception {
        var method = CartService.class.getDeclaredMethod("normalizeMoney", BigDecimal.class);
        method.setAccessible(true);

        BigDecimal result = (BigDecimal) method.invoke(cartService, new Object[]{null});
        assertEquals(BigDecimal.ZERO.setScale(2), result);
    }

    @Test
    void cartItem_getCartId_nullCart() {
        CartItem item = new CartItem();
        assertNull(item.getCartId());
    }

    @Test
    void orderItem_getOrderId_nullOrder() {
        OrderItem item = new OrderItem();
        assertNull(item.getOrderId());
    }

    @Test
    void viewCart_quoteSuccess_confirmedItemsBranch() {
        CartItem ci = new CartItem();
        ci.setItemName("Pizza");
        ci.setQty(2);

        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantName("Rest");

        ConfirmedItem confirmed = new ConfirmedItem();
        confirmed.setItemName("Pizza");
        confirmed.setQty(2);
        confirmed.setUnitPrice(BigDecimal.ONE);
        confirmed.setLineTotal(BigDecimal.valueOf(2));

        MenuReserveResponse quoteResp = new MenuReserveResponse();
        quoteResp.setSuccess(true);
        quoteResp.setConfirmedItems(List.of(confirmed));
        quoteResp.setSubtotal(BigDecimal.valueOf(2));

        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(ci));
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(res);
        when(restaurantClient.quote(any()))
                .thenReturn(quoteResp);

        CartViewResponse response = cartService.getActiveCart(customerId, null);

        assertEquals(BigDecimal.valueOf(2).setScale(2), response.getSubtotal());
    }

    @Test
    void viewCart_quoteNotSuccess_branch() {
        CartItem ci = new CartItem();
        ci.setItemName("Pizza");
        ci.setQty(1);

        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantName("Rest");

        MenuReserveResponse quoteResp = new MenuReserveResponse();
        quoteResp.setSuccess(false);

        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(ci));
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(res);
        when(restaurantClient.quote(any()))
                .thenReturn(quoteResp);

        CartViewResponse response = cartService.getActiveCart(customerId, null);

        assertEquals(BigDecimal.ZERO.setScale(2), response.getSubtotal());
    }

    @Test
    void viewCart_skipDelivery_branch() {
        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantName("Rest");
        res.setLocationName(""); // blank location

        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(new CartItem()));
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(res);

        CartViewResponse response = cartService.getActiveCart(customerId, null);

        assertEquals(BigDecimal.ZERO.setScale(2), response.getDeliveryCharge());
    }

    @Test
    void getActiveCart_blankRestaurantName_branch() {
        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of());
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(new RestaurantInfoResponse());

        assertNotNull(cartService.getActiveCart(customerId, "   "));
    }

    @Test
    void checkout_withRestaurantOverride_success() {
        CartItem ci = new CartItem();
        ci.setItemName("Pizza");
        ci.setQty(1);

        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantId(1L);
        res.setRestaurantName("Rest");

        Order order = new Order();
        order.setOrderId(UUID.randomUUID());

        when(restaurantClient.getRestaurantByName("Rest")).thenReturn(res);
        when(cartRepository.findByCustomerIdAndRestaurantIdAndStatus(any(), any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(ci));
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(res);
        when(orderService.placeOrder(any())).thenReturn(order);

        Order result = cartService.checkout(customerId, "User", "Rest");

        assertEquals(order.getOrderId(), result.getOrderId());
    }


    @Test
    void viewCart_quoteSuccess_butConfirmedItemsNull() {
        CartItem ci = new CartItem();
        ci.setItemName("Pizza");
        ci.setQty(1);

        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantName("Rest");

        MenuReserveResponse quoteResp = new MenuReserveResponse();
        quoteResp.setSuccess(true);
        quoteResp.setConfirmedItems(null); // important
        quoteResp.setSubtotal(BigDecimal.TEN);

        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(ci));
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(res);
        when(restaurantClient.quote(any()))
                .thenReturn(quoteResp);

        CartViewResponse response = cartService.getActiveCart(customerId, null);

        assertNotNull(response);
    }

    @Test
    void viewCart_deliveryResponseNull_branch() {
        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantName("Rest");
        res.setLocationName("Loc");

        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(new CartItem()));
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(res);
        when(deliveryClient.estimateDelivery(any()))
                .thenReturn(null); // important

        CartViewResponse response = cartService.getActiveCart(customerId, null);

        assertEquals(BigDecimal.ZERO.setScale(2), response.getDeliveryCharge());
    }

    @Test
    void viewCart_customerLocationBlank_branch() {
        cart.setDeliveryArea(""); // important

        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantName("Rest");
        res.setLocationName("Loc");

        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(new CartItem()));
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(res);

        CartViewResponse response = cartService.getActiveCart(customerId, null);

        assertEquals(BigDecimal.ZERO.setScale(2), response.getDeliveryCharge());
    }

    @Test
    void addItem_restaurantInfoNull_branch() {
        CartItemUpdateRequest req = new CartItemUpdateRequest();
        req.setRestaurantName("Rest");
        req.setItemName("Pizza");
        req.setQty(1);
        req.setDeliveryArea("Area1");

        when(restaurantClient.getRestaurantByName("Rest"))
                .thenReturn(new RestaurantInfoResponse()); // id null
        when(cartRepository.findByCustomerIdAndRestaurantIdAndStatus(any(), any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartIdAndItemNameIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of());

        assertNotNull(cartService.addOrUpdateItem(customerId, "User", req));
    }
    @Test
    void addItem_triggersNewCart_branch() {
        CartItemUpdateRequest req = new CartItemUpdateRequest();
        req.setRestaurantName("Rest");
        req.setItemName("Pizza");
        req.setQty(1);
        req.setDeliveryArea("Area1");

        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantId(1L);


        when(restaurantClient.getRestaurantByName("Rest")).thenReturn(res);
        when(cartRepository.findByCustomerIdAndRestaurantIdAndStatus(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(cartItemRepository.findByCart_CartIdAndItemNameIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of());
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(res);

        cartService.addOrUpdateItem(customerId, "User", req);

        verify(cartRepository, atLeastOnce()).save(any());
    }
    @Test
    void cartItem_getCartId_nonNullCart() {
        Cart cart = new Cart();
        UUID id = UUID.randomUUID();
        cart.setCartId(id);

        CartItem item = new CartItem();
        item.setCart(cart);

        assertEquals(id, item.getCartId());
    }

    @Test
    void viewCart_restaurantInfoNull_branch() {
        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(new CartItem()));
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(null); // important

        CartViewResponse response = cartService.getActiveCart(customerId, null);

        assertNotNull(response);
    }

    @Test
    void viewCart_restaurantLocationNull_branch() {
        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantName("Rest");
        res.setLocationName(null); // important

        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(new CartItem()));
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(res);

        CartViewResponse response = cartService.getActiveCart(customerId, null);

        assertEquals(BigDecimal.ZERO.setScale(2), response.getDeliveryCharge());
    }

    @Test
    void viewCart_quoteThrowsException_branch() {
        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(new CartItem()));
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(new RestaurantInfoResponse());
        when(restaurantClient.quote(any()))
                .thenThrow(new RuntimeException());

        CartViewResponse response = cartService.getActiveCart(customerId, null);

        assertNotNull(response);
    }

    @Test
    void viewCart_deliveryThrowsException_branch() {
        RestaurantInfoResponse res = new RestaurantInfoResponse();
        res.setRestaurantName("Rest");
        res.setLocationName("Loc");

        when(cartRepository.findByCustomerIdAndStatus(any(), any()))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(any()))
                .thenReturn(List.of(new CartItem()));
        when(restaurantClient.getRestaurantById(any()))
                .thenReturn(res);
        when(deliveryClient.estimateDelivery(any()))
                .thenThrow(new RuntimeException());

        CartViewResponse response = cartService.getActiveCart(customerId, null);

        assertEquals(BigDecimal.ZERO.setScale(2), response.getDeliveryCharge());
    }




}
