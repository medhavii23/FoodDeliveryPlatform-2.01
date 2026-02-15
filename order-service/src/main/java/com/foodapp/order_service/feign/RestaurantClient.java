package com.foodapp.order_service.feign;

import com.foodapp.order_service.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "RESTAURANT-SERVICE", configuration = com.foodapp.order_service.config.RestaurantClientConfig.class)
public interface RestaurantClient {

    @PostMapping("/api/restaurants/reserve")
    MenuReserveResponse reserve(@RequestBody MenuReserveRequest req);

    @PostMapping("/api/restaurants/release")
    MenuReserveResponse release(@RequestBody MenuReserveRequest req);

    @PostMapping("/api/restaurants/quote")
    MenuReserveResponse quote(@RequestBody MenuReserveRequest req);

    @GetMapping("/api/restaurants/search")
    RestaurantInfoResponse getRestaurantByName(@RequestParam String name);

    @GetMapping("/api/restaurants/{restaurantId}")
    RestaurantInfoResponse getRestaurantById(@PathVariable Long restaurantId);
}
