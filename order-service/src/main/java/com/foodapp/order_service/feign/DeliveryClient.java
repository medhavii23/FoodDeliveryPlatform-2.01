package com.foodapp.order_service.feign;

import com.foodapp.order_service.dto.DeliveryAssignRequest;
import com.foodapp.order_service.dto.DeliveryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "DELIVERY-SERVICE", configuration = com.foodapp.order_service.config.DeliveryClientConfig.class)
public interface DeliveryClient {

    @PostMapping("/api/delivery/estimate")
    DeliveryResponse estimateDelivery(@RequestBody DeliveryAssignRequest request);

    @PostMapping("/api/delivery/assign/{orderId}")
    DeliveryResponse assignDelivery(
            @PathVariable("orderId") UUID orderId,
            @RequestBody DeliveryAssignRequest request);
}
