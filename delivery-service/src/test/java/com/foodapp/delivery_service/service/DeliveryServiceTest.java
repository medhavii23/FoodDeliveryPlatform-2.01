package com.foodapp.delivery_service.service;

import com.foodapp.delivery_service.dto.DeliveryAssignRequest;
import com.foodapp.delivery_service.dto.DeliveryResponse;
import com.foodapp.delivery_service.exception.DeliveryNotFoundException;
import com.foodapp.delivery_service.feign.OrderClient;
import com.foodapp.delivery_service.model.Delivery;
import com.foodapp.delivery_service.model.DeliveryStatus;
import com.foodapp.delivery_service.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void estimateDelivery_withValidLocations_returnsChargeAndEta() {
        when(locationService.getLatLng("Anna Nagar")).thenReturn(new BigDecimal[]{BigDecimal.valueOf(13.0850), BigDecimal.valueOf(80.2101)});
        when(locationService.getLatLng("Adyar")).thenReturn(new BigDecimal[]{BigDecimal.valueOf(13.0012), BigDecimal.valueOf(80.2565)});
        DeliveryAssignRequest req = new DeliveryAssignRequest("Anna Nagar", "Adyar", null);

        DeliveryResponse resp = deliveryService.estimateDelivery(req);

        assertThat(resp.getDeliveryCharge()).isNotNull();
        assertThat(resp.getEta()).isNotBlank().contains("mins");
        assertThat(resp.isSuccess()).isTrue();
    }

    @Test
    void estimateDelivery_nullLocations_returnsZeroCharge() {
        DeliveryAssignRequest req = new DeliveryAssignRequest(null, null, null);
        DeliveryResponse resp = deliveryService.estimateDelivery(req);
        assertThat(resp.getDeliveryCharge()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.getEta()).isNotBlank();
    }

    @Test
    void track_whenDeliveryExists_returnsDelivery() {
        UUID orderId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setOrderId(orderId);
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));

        Delivery result = deliveryService.track(orderId);

        assertThat(result.getOrderId()).isEqualTo(orderId);
    }

    @Test
    void track_whenDeliveryNotFound_throws() {
        UUID orderId = UUID.randomUUID();
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.track(orderId))
                .isInstanceOf(DeliveryNotFoundException.class);
    }

    @Test
    void assignDeliveryPartner_withExplicitPartner_savesAndReturnsSuccess() {
        UUID orderId = UUID.randomUUID();
        DeliveryAssignRequest req = new DeliveryAssignRequest("Anna Nagar", "Adyar", "Arjun Kumar");
        when(locationService.getLatLng(any())).thenReturn(new BigDecimal[]{BigDecimal.valueOf(13), BigDecimal.valueOf(80)});
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryResponse resp = deliveryService.assignDeliveryPartner(orderId, req);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getPartnerName()).isEqualTo("Arjun Kumar");
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    void updateStatus_updatesAndSaves() {
        UUID orderId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setOrderId(orderId);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        Delivery result = deliveryService.updateStatus(orderId, DeliveryStatus.DELIVERED);

        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        verify(deliveryRepository).save(delivery);
    }
}
