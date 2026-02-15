package com.foodapp.order_service.repository;

import com.foodapp.order_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByRestaurantId(Long restaurantId);

    List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Optional<Order> findByOrderIdAndCustomerId(UUID orderId, UUID customerId);

    @Query(nativeQuery = true, value = """
        SELECT o.order_id, o.customer_id, o.total_amount, o.status, o.created_at,
               r.restaurant_id, r.restaurant_name, r.location_name,
               d.delivery_id, d.partner_name, d.eta, d.status AS delivery_status
        FROM order_schema.orders o
        JOIN restaurant.restaurants r ON r.restaurant_id = o.restaurant_id
        LEFT JOIN delivery.deliveries d ON d.order_id = o.order_id
        WHERE o.customer_id = :customerId
        ORDER BY o.created_at DESC
        """)
    List<Object[]> findCustomerOrderSummaryByCustomerId(@Param("customerId") UUID customerId);
}
