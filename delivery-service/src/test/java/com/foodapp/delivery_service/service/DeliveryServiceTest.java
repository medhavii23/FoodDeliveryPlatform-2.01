package com.foodapp.delivery_service.service;

import com.foodapp.delivery_service.constants.Constants;
import com.foodapp.delivery_service.dto.DeliveryAssignRequest;
import com.foodapp.delivery_service.dto.DeliveryResponse;
import com.foodapp.delivery_service.dto.OrderSyncRequest;
import com.foodapp.delivery_service.exception.DeliveryNotFoundException;
import com.foodapp.delivery_service.feign.OrderClient;
import com.foodapp.delivery_service.model.Delivery;
import com.foodapp.delivery_service.model.DeliveryStatus;
import com.foodapp.delivery_service.repository.DeliveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private LocationService locationService;

    @Mock
    private OrderClient orderClient;

    @Mock
    private PartnerNameGenerator partnerNameGenerator;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    void testEstimateDelivery_Success() {
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", null);
        when(locationService.getLatLng("Loc A")).thenReturn(new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
        when(locationService.getLatLng("Loc B"))
                .thenReturn(new BigDecimal[] { BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.1) }); // Approx 15km

        DeliveryResponse resp = deliveryService.estimateDelivery(req);

        assertNotNull(resp);
        assertThat(resp.getDeliveryCharge()).isNotNull();
        assertEquals(Constants.ASSIGN_SUCCESS, resp.getMessage());
    }


    @Test
    void testEstimateDelivery_NullLocations() {
        // rLoc null triggers outer else branch -> charge = 0 scaled
        DeliveryAssignRequest req = new DeliveryAssignRequest(null, "Loc B", null);

        DeliveryResponse resp = deliveryService.estimateDelivery(req);

        assertNotNull(resp);
        assertEquals(BigDecimal.ZERO.setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING), resp.getDeliveryCharge());
        assertEquals(Constants.NA, resp.getEta());
    }

    @Test
    void testEstimateDelivery_FreeDeliveryBranch() {
        // Distance extremely small -> <= FREE_DELIVERY_DISTANCE branch
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", null);

        when(locationService.getLatLng("Loc A"))
                .thenReturn(new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        when(locationService.getLatLng("Loc B"))
                .thenReturn(new BigDecimal[]{BigDecimal.valueOf(0.0001), BigDecimal.valueOf(0.0001)});

        DeliveryResponse resp = deliveryService.estimateDelivery(req);

        assertNotNull(resp);
        assertEquals(BigDecimal.ZERO.setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING), resp.getDeliveryCharge());
        assertTrue(resp.getEta().contains("mins"));
    }

    @Test
    void testEstimateDelivery_CoordinatesMissing_ValidateCoordsThrows() {
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", null);

        // validateCoords should throw -> caught -> charge reset to 0 scaled
        when(locationService.getLatLng("Loc A"))
                .thenReturn(new BigDecimal[]{null, BigDecimal.ZERO});
        when(locationService.getLatLng("Loc B"))
                .thenReturn(new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});

        DeliveryResponse resp = deliveryService.estimateDelivery(req);

        assertNotNull(resp);
        assertEquals(BigDecimal.ZERO.setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING), resp.getDeliveryCharge());
        assertEquals(Constants.NA, resp.getEta()); // because exception happens before ETA assignment
    }

    @Test
    void testAssignDeliveryPartner_ExistingDelivery_UpdatesSameObject() {
        UUID orderId = UUID.randomUUID();

        Delivery existingDelivery = new Delivery();
        existingDelivery.setOrderId(orderId);
        existingDelivery.setPartnerName("OLD");
        existingDelivery.setStatus(DeliveryStatus.PENDING);

        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", "Partner X");

        when(locationService.getLatLng(anyString()))
                .thenReturn(new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingDelivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse resp = deliveryService.assignDeliveryPartner(orderId, req);

        assertNotNull(resp);
        assertEquals("Partner X", resp.getPartnerName());

        // This is the branch: existing.orElse(new Delivery())
        verify(deliveryRepository).save(same(existingDelivery));
        assertEquals(DeliveryStatus.ASSIGNED, existingDelivery.getStatus());
    }

    @Test
    void testAssignDeliveryPartner_AutoAssignFail_PendingBranch() {
        UUID orderId = UUID.randomUUID();
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", null);

        when(locationService.getLatLng(anyString()))
                .thenReturn(new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        when(partnerNameGenerator.randomPartnerName()).thenReturn(null); // simulate 10% fail
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse resp = deliveryService.assignDeliveryPartner(orderId, req);

        assertNotNull(resp);
        assertNull(resp.getPartnerName());
        assertEquals("Pending manual assignment", resp.getMessage());

        // verify saved delivery status PENDING
        verify(deliveryRepository).save(argThat(d -> d.getStatus() == DeliveryStatus.PENDING));
    }

    @Test
    void testAssignDeliveryPartner_OrderSyncThrows_IsCaught() {
        UUID orderId = UUID.randomUUID();
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", "Partner 1");

        when(locationService.getLatLng(anyString()))
                .thenReturn(new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        doThrow(new RuntimeException("sync failed")).when(orderClient).syncOrder(any());

        // should NOT throw, because catch is present
        DeliveryResponse resp = deliveryService.assignDeliveryPartner(orderId, req);

        assertNotNull(resp);
        assertEquals("Partner 1", resp.getPartnerName());
        verify(orderClient).syncOrder(any());
    }


    @Test
    void testEstimateDelivery_Error() {
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", null);
        when(locationService.getLatLng("Loc A")).thenThrow(new RuntimeException("Error"));

        DeliveryResponse resp = deliveryService.estimateDelivery(req);

        assertNotNull(resp);
        assertEquals(BigDecimal.ZERO.setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING), resp.getDeliveryCharge());

    }

    @Test
    void testAssignDeliveryPartner_Manual() {
        UUID orderId = UUID.randomUUID();
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", "Partner 1");

        when(locationService.getLatLng(anyString())).thenReturn(new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse resp = deliveryService.assignDeliveryPartner(orderId, req);

        assertNotNull(resp);
        assertEquals("Partner 1", resp.getPartnerName());
        verify(orderClient).syncOrder(any());
    }

    @Test
    void testAssignDeliveryPartner_Auto() {
        UUID orderId = UUID.randomUUID();
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", null);

        when(locationService.getLatLng(anyString())).thenReturn(new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
        when(partnerNameGenerator.randomPartnerName()).thenReturn("Auto Partner");
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse resp = deliveryService.assignDeliveryPartner(orderId, req);

        assertEquals("Auto Partner", resp.getPartnerName());
    }

    @Test
    void testTrack_Found() {
        UUID orderId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));

        Delivery result = deliveryService.track(orderId);
        assertNotNull(result);
    }

    @Test
    void testTrack_NotFound() {
        UUID orderId = UUID.randomUUID();
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(DeliveryNotFoundException.class, () -> deliveryService.track(orderId));
    }

    @Test
    void testUpdateStatus() {
        UUID orderId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

        Delivery result = deliveryService.updateStatus(orderId, DeliveryStatus.DELIVERED);
        assertEquals(DeliveryStatus.DELIVERED, result.getStatus());
    }

    @Test
    void testEstimateDelivery_CustomerLocationNull_branch() {
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", null, null);

        DeliveryResponse resp = deliveryService.estimateDelivery(req);

        assertNotNull(resp);
        assertEquals(BigDecimal.ZERO.setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING), resp.getDeliveryCharge());
        assertEquals(Constants.NA, resp.getEta());
    }

    @Test
    void testAssignDeliveryPartner_partnerBlank_triggersAutoAssignBranch() {
        UUID orderId = UUID.randomUUID();
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", "   "); // not null, blank

        when(locationService.getLatLng(anyString()))
                .thenReturn(new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        when(partnerNameGenerator.randomPartnerName()).thenReturn("Auto Partner From Blank");
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse resp = deliveryService.assignDeliveryPartner(orderId, req);

        assertNotNull(resp);
        assertEquals("Auto Partner From Blank", resp.getPartnerName());
        verify(partnerNameGenerator).randomPartnerName();
    }

    @Test
    void testAssignDeliveryPartner_syncOrderSuccess_branch() {
        UUID orderId = UUID.randomUUID();
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", "Partner OK");

        when(locationService.getLatLng(anyString()))
                .thenReturn(new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse resp = deliveryService.assignDeliveryPartner(orderId, req);

        assertNotNull(resp);
        assertEquals("Partner OK", resp.getPartnerName());
        verify(orderClient, times(1)).syncOrder(any(OrderSyncRequest.class));
    }

    @Test
    void cover_private_calculateDistance_viaReflection() throws Exception {
        Method m = DeliveryService.class.getDeclaredMethod(
                "calculateDistance", double.class, double.class, double.class, double.class);
        m.setAccessible(true);

        double d = (double) m.invoke(deliveryService, 13.0, 80.0, 13.0, 80.0);
        assertThat(d).isEqualTo(0.0);
    }

    @Test
    void cover_private_validateCoords_throws_viaReflection() throws Exception {
        Method m = DeliveryService.class.getDeclaredMethod(
                "validateCoords", BigDecimal.class, BigDecimal.class, String.class);
        m.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                m.invoke(deliveryService, null, BigDecimal.ONE, "restaurant");
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw (RuntimeException) e.getCause();
            }
        }).isInstanceOf(RuntimeException.class)
                .hasMessageContaining(Constants.COORDINATES_MISSING);
    }

    @Test
    void cover_private_validateCoords_ok_branch_viaReflection() throws Exception {
        Method m = DeliveryService.class.getDeclaredMethod(
                "validateCoords", BigDecimal.class, BigDecimal.class, String.class);
        m.setAccessible(true);

        assertDoesNotThrow(() ->
                m.invoke(deliveryService, BigDecimal.ONE, BigDecimal.ONE, "restaurant")
        );
    }


    @Test
    void cover_private_calculateDistance_nonZero_viaReflection() throws Exception {
        Method m = DeliveryService.class.getDeclaredMethod(
                "calculateDistance", double.class, double.class, double.class, double.class);
        m.setAccessible(true);

        double d = (double) m.invoke(deliveryService, 13.0, 80.0, 13.1, 80.1);
        assertThat(d).isGreaterThan(0.0);
    }

    @Test
    void testEstimateDelivery_BothLocationsNull_branch() {
        DeliveryAssignRequest req = new DeliveryAssignRequest(null, null, null);

        DeliveryResponse resp = deliveryService.estimateDelivery(req);

        assertNotNull(resp);
        assertEquals(
                BigDecimal.ZERO.setScale(Constants.MONEY_SCALE, Constants.MONEY_ROUNDING),
                resp.getDeliveryCharge()
        );
        assertEquals(Constants.NA, resp.getEta());
    }



}
