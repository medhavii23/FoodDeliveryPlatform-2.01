package com.foodapp.delivery_service.feign;

import com.foodapp.delivery_service.dto.OrderSyncRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ORDER-SERVICE")
public interface OrderClient {
    @PostMapping("/api/cart/orders/sync")
    void syncOrder(@RequestBody OrderSyncRequest syncReq);
}
