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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private OrderService orderService;

    @Mock
    private RestaurantClient restaurantClient;

    @Mock
    private DeliveryClient deliveryClient;

    @InjectMocks
    private CartService cartService;

    @Test
    void testAddOrUpdateItem_NewCart() {
        UUID customerId = UUID.randomUUID();
        String restaurantName = "Test Rest";
        CartItemUpdateRequest request = new CartItemUpdateRequest();
        request.setRestaurantName(restaurantName);
        request.setItemName("Pizza");
        request.setQty(2);
        request.setDeliveryArea("Area 1");

        RestaurantInfoResponse restaurantInfo = new RestaurantInfoResponse();
        restaurantInfo.setRestaurantId(1L);
        restaurantInfo.setRestaurantName(restaurantName);

        when(restaurantClient.getRestaurantByName(restaurantName)).thenReturn(restaurantInfo);
        when(cartRepository.findByCustomerIdAndRestaurantIdAndStatus(customerId, 1L, CartStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        // For buildViewData
        when(restaurantClient.getRestaurantById(any())).thenReturn(restaurantInfo);
        when(cartItemRepository.findByCart_CartId(any())).thenReturn(Collections.emptyList());
        // NOTE: In the actual code, we save the Item, then call toAddToCartView ->
        // buildViewData -> repo.findByCartId
        // Since we mock repo.findByCartId to return empty, the view will be empty.
        // That's acceptable for this unit test if we verify the save interactions.

        cartService.addOrUpdateItem(customerId, "User", request);

        verify(cartRepository).save(any(Cart.class));
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void testGetActiveCart_Found() {
        UUID customerId = UUID.randomUUID();
        Cart cart = new Cart();
        cart.setCartId(UUID.randomUUID());
        cart.setRestaurantId(1L);
        cart.setStatus(CartStatus.ACTIVE);

        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        RestaurantInfoResponse resInfo = new RestaurantInfoResponse();
        resInfo.setRestaurantName("Rest 1");
        when(restaurantClient.getRestaurantById(1L)).thenReturn(resInfo);

        CartViewResponse response = cartService.getActiveCart(customerId, null);

        assertNotNull(response);
        assertEquals(cart.getCartId(), response.getCartId());
    }

    @Test
    void testCheckout_Success() {
        UUID customerId = UUID.randomUUID();
        Cart cart = new Cart();
        cart.setCartId(UUID.randomUUID());
        cart.setRestaurantId(1L);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setDeliveryArea("Area 1");

        CartItem item = new CartItem();
        item.setItemName("Pizza");
        item.setQty(2);

        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(cart.getCartId())).thenReturn(List.of(item));

        RestaurantInfoResponse resInfo = new RestaurantInfoResponse();
        resInfo.setRestaurantName("Rest 1");
        when(restaurantClient.getRestaurantById(1L)).thenReturn(resInfo);

        Order order = new Order();
        order.setOrderId(UUID.randomUUID());
        when(orderService.placeOrder(any(PlaceOrderRequest.class))).thenReturn(order);

        Order result = cartService.checkout(customerId, "User", null);

        assertNotNull(result);
        assertEquals(CartStatus.CHECKED_OUT, cart.getStatus());
        assertEquals(order.getOrderId(), cart.getOrderId());
    }

    @Test
    void testCheckout_EmptyCart() {
        UUID customerId = UUID.randomUUID();
        Cart cart = new Cart();
        cart.setCartId(UUID.randomUUID());
        cart.setStatus(CartStatus.ACTIVE);

        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(cart.getCartId())).thenReturn(Collections.emptyList());

        assertThrows(IllegalStateException.class, () -> cartService.checkout(customerId, "User", null));
    }
}
