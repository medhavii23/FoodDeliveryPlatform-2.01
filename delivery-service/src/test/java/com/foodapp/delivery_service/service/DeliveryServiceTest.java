package com.foodapp.delivery_service.service;

import com.foodapp.delivery_service.constants.Constants;
import com.foodapp.delivery_service.dto.DeliveryAssignRequest;
import com.foodapp.delivery_service.dto.DeliveryResponse;
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
        assertEquals(Constants.ASSIGN_SUCCESS, resp.getMessage());
    }

    @Test
    void testEstimateDelivery_Error() {
        DeliveryAssignRequest req = new DeliveryAssignRequest("Loc A", "Loc B", null);
        when(locationService.getLatLng("Loc A")).thenThrow(new RuntimeException("Error"));

        DeliveryResponse resp = deliveryService.estimateDelivery(req);

        assertNotNull(resp);
        assertEquals(BigDecimal.ZERO.setScale(2), resp.getDeliveryCharge());
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
}
