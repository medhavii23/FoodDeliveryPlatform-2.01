package com.foodapp.order_service.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void ensureCustomerExists_whenExists_returnsCustomer() {
        UUID id = UUID.randomUUID();
        Customer c = new Customer(id);
        when(customerRepository.findById(id)).thenReturn(Optional.of(c));

        Customer result = customerService.ensureCustomerExists(id, "Alice");

        assertThat(result.getCustomerId()).isEqualTo(id);
        verify(customerRepository).findById(id);
    }

    @Test
    void ensureCustomerExists_whenNotExists_createsAndSaves() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

        Customer result = customerService.ensureCustomerExists(id, "Bob");

        assertThat(result.getCustomerId()).isEqualTo(id);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void verifyOrThrow_whenNullCustomerId_throws() {
        assertThatThrownBy(() -> customerService.verifyOrThrow(null, "x"))
                .isInstanceOf(InvalidCustomerCredentialsException.class);
    }

    @Test
    void verifyOrThrow_whenNotFound_throws() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.verifyOrThrow(id, "x"))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void verifyOrThrow_whenFound_returnsCustomer() {
        UUID id = UUID.randomUUID();
        Customer c = new Customer(id);
        when(customerRepository.findById(id)).thenReturn(Optional.of(c));

        Customer result = customerService.verifyOrThrow(id, "Alice");

        assertThat(result.getCustomerId()).isEqualTo(id);
    }
}
