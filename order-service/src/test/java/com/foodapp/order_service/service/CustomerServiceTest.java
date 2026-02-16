package com.foodapp.order_service.service;

import com.foodapp.order_service.constants.ErrorMessages;
import com.foodapp.order_service.exception.CustomerNotFoundException;
import com.foodapp.order_service.exception.InvalidCustomerCredentialsException;
import com.foodapp.order_service.model.Customer;
import com.foodapp.order_service.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void testEnsureCustomerExists_Existing() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer(customerId);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        Customer result = customerService.ensureCustomerExists(customerId, "Test Name");

        assertEquals(customer, result);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void testEnsureCustomerExists_New() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer result = customerService.ensureCustomerExists(customerId, "Test Name");

        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void testVerifyOrThrow_Success() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer(customerId);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        Customer result = customerService.verifyOrThrow(customerId, "Test Name");

        assertEquals(customer, result);
    }

    @Test
    void testVerifyOrThrow_NullId() {
        assertThrows(InvalidCustomerCredentialsException.class, () -> customerService.verifyOrThrow(null, "Test Name"));
    }

    @Test
    void testVerifyOrThrow_NotFound() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> customerService.verifyOrThrow(customerId, "Test Name"));
    }
}
