package com.foodapp.order_service.service;

import com.foodapp.order_service.constants.Constants;
import com.foodapp.order_service.dto.*;
import com.foodapp.order_service.exception.InsufficientStockException;
import com.foodapp.order_service.exception.OrderNotFoundException;
import com.foodapp.order_service.exception.OrderProcessingException;
import com.foodapp.order_service.feign.DeliveryClient;
import com.foodapp.order_service.feign.RestaurantClient;
import com.foodapp.order_service.model.*;
import com.foodapp.order_service.repository.OrderItemRepository;
import com.foodapp.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock RestaurantClient restaurantClient;
    @Mock DeliveryClient deliveryClient;
    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock CustomerService customerService;

    @InjectMocks OrderService orderService;

    private PlaceOrderRequest baseRequest;

    @BeforeEach
    void setup() {
        baseRequest = new PlaceOrderRequest();
        baseRequest.setCustomerId(UUID.randomUUID());
        baseRequest.setCustomerName("Medhavi");
        baseRequest.setRestaurantName("Rest");
        baseRequest.setDeliveryArea("Area");
        baseRequest.setItems(List.of());
    }

    /* ================= placeOrder ================= */

    private MenuReserveResponse successReserve() {
        MenuReserveResponse r = new MenuReserveResponse();
        r.setSuccess(true);
        r.setSubtotal(BigDecimal.TEN);
        r.setLocationName("Loc");
        r.setRestaurantId(1L);
        return r;
    }

    @Test
    void placeOrder_partnerAssigned() {
        when(restaurantClient.reserve(any())).thenReturn(successReserve());
        when(deliveryClient.assignDelivery(any(), any()))
                .thenReturn(new DeliveryResponse("P1", BigDecimal.ONE, "30m", true, null));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.placeOrder(baseRequest);

        assertEquals(OrderStatus.PLACED, order.getStatus());
    }

    @Test
    void placeOrder_partnerPending() {
        when(restaurantClient.reserve(any())).thenReturn(successReserve());
        when(deliveryClient.assignDelivery(any(), any()))
                .thenReturn(new DeliveryResponse(null, BigDecimal.ONE, "30m", true, null));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.placeOrder(baseRequest);

        assertEquals(OrderStatus.PREPARING, order.getStatus());
    }

    @Test
    void placeOrder_deliveryServiceFailure_usesFallback() {
        when(restaurantClient.reserve(any())).thenReturn(successReserve());
        when(deliveryClient.assignDelivery(any(), any()))
                .thenThrow(new RuntimeException());
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.placeOrder(baseRequest);

        assertEquals(OrderStatus.PREPARING, order.getStatus());
    }

    @Test
    void placeOrder_deliveryResponseNotSuccess_fallback() {
        when(restaurantClient.reserve(any())).thenReturn(successReserve());
        when(deliveryClient.assignDelivery(any(), any()))
                .thenReturn(new DeliveryResponse(null, BigDecimal.ZERO, "0", false, "fail"));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.placeOrder(baseRequest);

        assertEquals(OrderStatus.PREPARING, order.getStatus());
    }

    @Test
    void placeOrder_confirmedItems_saved() {
        MenuReserveResponse reserve = successReserve();

        ConfirmedItem ci = new ConfirmedItem();
        ci.setItemId(1L);
        ci.setItemName("Burger");
        ci.setQty(2);
        ci.setUnitPrice(BigDecimal.ONE);
        ci.setLineTotal(BigDecimal.valueOf(2));
        reserve.setConfirmedItems(List.of(ci));

        when(restaurantClient.reserve(any())).thenReturn(reserve);
        when(deliveryClient.assignDelivery(any(), any()))
                .thenReturn(new DeliveryResponse("P", BigDecimal.ONE, "30", true, null));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        orderService.placeOrder(baseRequest);

        verify(orderItemRepository).save(any());
    }



    /* ================= reserveRestaurantInventory ================= */

    @Test
    void reserve_nullResponse() {
        when(restaurantClient.reserve(any())).thenReturn(null);
        assertThrows(InsufficientStockException.class,
                () -> orderService.placeOrder(baseRequest));
    }

    @Test
    void reserve_itemNotFound() {
        MenuReserveResponse r = new MenuReserveResponse();
        r.setSuccess(false);

        UnavailableItem ui = new UnavailableItem();
        ui.setAvailableQty(0);
        ui.setItemId(null);

        r.setUnavailableItems(List.of(ui));
        when(restaurantClient.reserve(any())).thenReturn(r);

        assertThrows(InsufficientStockException.class,
                () -> orderService.placeOrder(baseRequest));
    }

    @Test
    void reserve_insufficientStock() {
        MenuReserveResponse r = new MenuReserveResponse();
        r.setSuccess(false);

        UnavailableItem ui = new UnavailableItem();
        ui.setAvailableQty(1);
        ui.setItemId(1L);

        r.setUnavailableItems(List.of(ui));
        when(restaurantClient.reserve(any())).thenReturn(r);

        assertThrows(InsufficientStockException.class,
                () -> orderService.placeOrder(baseRequest));
    }

    @Test
    void reserve_blankMessageFallback() {
        MenuReserveResponse r = new MenuReserveResponse();
        r.setSuccess(false);
        r.setMessage(" ");
        when(restaurantClient.reserve(any())).thenReturn(r);

        assertThrows(InsufficientStockException.class,
                () -> orderService.placeOrder(baseRequest));
    }

    /* ================= updateOrderStatus ================= */

    @Test
    void updateOrderStatus_sameStatus() {
        Order o = new Order();
        o.setStatus(OrderStatus.PLACED);

        when(orderRepository.findById(any())).thenReturn(Optional.of(o));

        assertEquals(OrderStatus.PLACED,
                orderService.updateOrderStatus(UUID.randomUUID(), OrderStatus.PLACED).getStatus());
    }

    @Test
    void updateOrderStatus_changeStatus() {
        Order o = new Order();
        o.setStatus(OrderStatus.PLACED);

        when(orderRepository.findById(any())).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenReturn(o);

        assertEquals(OrderStatus.PREPARING,
                orderService.updateOrderStatus(UUID.randomUUID(), OrderStatus.PREPARING).getStatus());
    }

    /* ================= syncOrder ================= */

    @Test
    void syncOrder_deliveryChargeOnly() {
        Order o = new Order();
        o.setFoodAmount(BigDecimal.TEN);

        when(orderRepository.findById(any())).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenReturn(o);

        OrderSyncRequest req = new OrderSyncRequest();
        req.setOrderId(UUID.randomUUID());
        req.setDeliveryCharge(BigDecimal.ONE);

        assertNotNull(orderService.syncOrder(req).getTotalAmount());
    }

    @Test
    void syncOrder_statusUpdateBranch() {
        Order o = new Order();
        o.setStatus(OrderStatus.PLACED);

        when(orderRepository.findById(any())).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenReturn(o);

        OrderSyncRequest req = new OrderSyncRequest();
        req.setOrderId(UUID.randomUUID());
        req.setStatus(OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, orderService.syncOrder(req).getStatus());
    }

    @Test
    void syncOrder_partnerAutoPlaced() {
        Order o = new Order();
        o.setStatus(OrderStatus.PREPARING);

        when(orderRepository.findById(any())).thenReturn(Optional.of(o));

        OrderSyncRequest req = new OrderSyncRequest();
        req.setOrderId(UUID.randomUUID());
        req.setPartnerName("P");

        assertEquals(OrderStatus.PLACED, orderService.syncOrder(req).getStatus());
    }

    @Test
    void syncOrder_noUpdates() {
        when(orderRepository.findById(any())).thenReturn(Optional.of(new Order()));

        OrderSyncRequest req = new OrderSyncRequest();
        req.setOrderId(UUID.randomUUID());

        assertNotNull(orderService.syncOrder(req));
    }

    @Test
    void syncOrder_notFound() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        OrderSyncRequest req = new OrderSyncRequest();
        req.setOrderId(UUID.randomUUID());

        assertThrows(RuntimeException.class,
                () -> orderService.syncOrder(req));
    }

    /* ================= getUserOrders ================= */

    @Test
    void getUserOrders_returnsList() {
        when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(new Order()));

        assertEquals(1, orderService.getUserOrders(UUID.randomUUID()).size());
    }

    /* ================= getUserOrder ================= */

    @Test
    void getUserOrder_notFound() {
        when(orderRepository.findByOrderIdAndCustomerId(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.getUserOrder(UUID.randomUUID(), UUID.randomUUID()));
    }

    /* ================= customer summary mapping ================= */

    @Test
    void customerSummary_timestampBranch() {
        Object[] row = new Object[]{
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "PLACED",
                Timestamp.from(Instant.now()),
                1L,"R","L",1L,"P","30","DELIVERED"
        };

        when(orderRepository.findCustomerOrderSummaryByCustomerId(any()))
                .thenReturn(Collections.singletonList(row));

        assertEquals(1, orderService.getCustomerOrderSummary(UUID.randomUUID()).size());
    }

    @Test
    void customerSummary_instantBranch() {
        Object[] row = new Object[]{
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "PLACED",
                Instant.now(),
                1L,"R","L",1L,"P","30","DELIVERED"
        };

        when(orderRepository.findCustomerOrderSummaryByCustomerId(any()))
                .thenReturn(Collections.singletonList(row));

        assertEquals(1, orderService.getCustomerOrderSummary(UUID.randomUUID()).size());
    }

    @Test
    void customerSummary_stringTimestampBranch() {
        Object[] row = new Object[]{
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "PLACED",
                "2024-01-01 10:00:00",
                1L,"R","L",1L,"P","30","DELIVERED"
        };

        when(orderRepository.findCustomerOrderSummaryByCustomerId(any()))
                .thenReturn(Collections.singletonList(row));

        assertEquals(1, orderService.getCustomerOrderSummary(UUID.randomUUID()).size());
    }

    @Test
    void customerSummary_empty() {
        when(orderRepository.findCustomerOrderSummaryByCustomerId(any()))
                .thenReturn(Collections.emptyList());

        assertTrue(orderService.getCustomerOrderSummary(UUID.randomUUID()).isEmpty());
    }

    /* ================= normalizeMoney ================= */

    @Test
    void normalizeMoney_nullHandled() {
        BigDecimal r = orderService.normalizeMoney(null);
        assertEquals(BigDecimal.ZERO.setScale(Constants.MONEY_SCALE), r);
    }

    @Test
    void normalizeMoney_valueProvided() {
        BigDecimal result = orderService.normalizeMoney(BigDecimal.ONE);
        assertEquals(Constants.MONEY_SCALE, result.scale());
    }
    @Test
    void placeOrder_saveFails_triggersRollback_and_releaseExceptionIgnored() {

        // Step 1: Reservation success
        when(restaurantClient.reserve(any()))
                .thenReturn(successReserve());

        // Step 2: Delivery success (DO NOT throw here)
        when(deliveryClient.assignDelivery(any(), any()))
                .thenReturn(new DeliveryResponse("P",
                        BigDecimal.ONE,
                        "30m",
                        true,
                        null));

        // Step 3: Force failure in saveOrderWithItems
        when(orderRepository.save(any()))
                .thenThrow(new RuntimeException("DB failure"));

        // Step 4: Simulate release() also failing
        doThrow(new RuntimeException("Release failure"))
                .when(restaurantClient).release(any());

        // Expect rollback exception
        assertThrows(OrderProcessingException.class,
                () -> orderService.placeOrder(baseRequest));

        // Verify rollback attempted
        verify(restaurantClient).release(any());
    }




    @Test
    void placeOrder_deliveryResponseNull_usesFallback() {
        when(restaurantClient.reserve(any())).thenReturn(successReserve());
        when(deliveryClient.assignDelivery(any(), any()))
                .thenReturn(null);

        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.placeOrder(baseRequest);

        assertEquals(OrderStatus.PREPARING, order.getStatus());
    }
    @Test
    void reserve_nullMessageFallback() {
        MenuReserveResponse r = new MenuReserveResponse();
        r.setSuccess(false);
        r.setMessage(null);

        when(restaurantClient.reserve(any())).thenReturn(r);

        assertThrows(InsufficientStockException.class,
                () -> orderService.placeOrder(baseRequest));
    }

    @Test
    void updateOrderStatus_notFound() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.updateOrderStatus(UUID.randomUUID(), OrderStatus.PLACED));
    }

    @Test
    void getUserOrder_success() {
        Order o = new Order();

        when(orderRepository.findByOrderIdAndCustomerId(any(), any()))
                .thenReturn(Optional.of(o));

        assertNotNull(orderService.getUserOrder(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void reserve_unavailableItemsNull() {
        MenuReserveResponse r = new MenuReserveResponse();
        r.setSuccess(false);
        r.setUnavailableItems(null);

        when(restaurantClient.reserve(any())).thenReturn(r);

        assertThrows(InsufficientStockException.class,
                () -> orderService.placeOrder(baseRequest));
    }

    @Test
    void releaseInventory_exceptionIgnored() {

        // Step 1: Reservation success
        when(restaurantClient.reserve(any()))
                .thenReturn(successReserve());

        // Step 2: Delivery success (DO NOT throw here)
        when(deliveryClient.assignDelivery(any(), any()))
                .thenReturn(new DeliveryResponse("P",
                        BigDecimal.ONE,
                        "30m",
                        true,
                        null));

        // Step 3: Force failure during save
        when(orderRepository.save(any()))
                .thenThrow(new RuntimeException("DB failure"));

        // Step 4: Simulate release also failing
        doThrow(new RuntimeException("Release failed"))
                .when(restaurantClient).release(any());

        // Now outer catch block executes
        assertThrows(OrderProcessingException.class,
                () -> orderService.placeOrder(baseRequest));

        // Verify rollback was attempted
        verify(restaurantClient).release(any());
    }


    @Test
    void customerSummary_allNullRow() {
        Object[] row = new Object[12];

        when(orderRepository.findCustomerOrderSummaryByCustomerId(any()))
                .thenReturn(Collections.singletonList(row));

        assertEquals(1, orderService.getCustomerOrderSummary(UUID.randomUUID()).size());
    }



}
