package com.foodapp.order_service.repository;

import com.foodapp.order_service.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}
