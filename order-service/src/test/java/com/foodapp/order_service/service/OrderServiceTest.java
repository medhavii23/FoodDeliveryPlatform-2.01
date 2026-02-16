package com.foodapp.order_service.service;

import com.foodapp.order_service.constants.Constants;
import com.foodapp.order_service.dto.*;
import com.foodapp.order_service.exception.InsufficientStockException;
import com.foodapp.order_service.exception.OrderNotFoundException;
import com.foodapp.order_service.feign.DeliveryClient;
import com.foodapp.order_service.feign.RestaurantClient;
import com.foodapp.order_service.model.Order;
import com.foodapp.order_service.model.OrderStatus;
import com.foodapp.order_service.repository.OrderItemRepository;
import com.foodapp.order_service.repository.OrderRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private RestaurantClient restaurantClient;

    @Mock
    private DeliveryClient deliveryClient;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void testPlaceOrder_Success() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setCustomerId(UUID.randomUUID());
        request.setRestaurantName("Rest 1");
        request.setDeliveryArea("Area 1");

        MenuReserveResponse reserveResp = new MenuReserveResponse();
        reserveResp.setSuccess(true);
        reserveResp.setSubtotal(BigDecimal.TEN);
        reserveResp.setLocationName("Loc A");

        DeliveryResponse deliveryResp = new DeliveryResponse();
        deliveryResp.setSuccess(true);
        deliveryResp.setDeliveryCharge(BigDecimal.ONE);
        deliveryResp.setPartnerName("Partner A");

        when(restaurantClient.reserve(any())).thenReturn(reserveResp);
        when(deliveryClient.assignDelivery(any(), any())).thenReturn(deliveryResp);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.placeOrder(request);

        assertNotNull(order);
        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertNotNull(order.getTotalAmount());
        verify(customerService).ensureCustomerExists(any(), any());
    }

    @Test
    void testPlaceOrder_InsufficientStock() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setRestaurantName("Rest 1");

        MenuReserveResponse reserveResp = new MenuReserveResponse();
        reserveResp.setSuccess(false); // Failed reservation

        when(restaurantClient.reserve(any())).thenReturn(reserveResp);

        assertThrows(InsufficientStockException.class, () -> orderService.placeOrder(request));
    }

    @Test
    void testUpdateOrderStatus() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setOrderId(orderId);
        order.setStatus(OrderStatus.PLACED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order updated = orderService.updateOrderStatus(orderId, OrderStatus.PREPARED);

        assertEquals(OrderStatus.PREPARED, updated.getStatus());
    }

    @Test
    void testSyncOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setOrderId(orderId);
        order.setStatus(OrderStatus.PREPARING);

        OrderSyncRequest syncReq = new OrderSyncRequest();
        syncReq.setOrderId(orderId);
        syncReq.setPartnerName("Partner X"); // Should trigger PLACED

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.syncOrder(syncReq);

        assertEquals(OrderStatus.PLACED, result.getStatus());
    }

    @Test
    void testGetUserOrders() {
        UUID customerId = UUID.randomUUID();
        when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).thenReturn(Collections.emptyList());

        List<Order> orders = orderService.getUserOrders(customerId);
        assertNotNull(orders);
    }
}
