package com.foodapp.order_service.service;

import com.foodapp.order_service.constants.Constants;
import com.foodapp.order_service.constants.ErrorMessages;
import com.foodapp.order_service.exception.CustomerNotFoundException;
import com.foodapp.order_service.exception.InvalidCustomerCredentialsException;
import com.foodapp.order_service.model.Customer;
import com.foodapp.order_service.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service responsible for customer validation and persistence logic
 * within the Order Service.
 *
 * <p>This service ensures:
 * <ul>
 *     <li>Customers exist before performing order/cart operations</li>
 *     <li>Customer identity validation during authenticated flows</li>
 *     <li>Controlled exception handling for invalid or missing customers</li>
 * </ul>
 *
 * <p>This class is typically invoked by CartService and OrderService
 * before allowing business operations.
 */
@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Ensures that a customer exists in the database.
     *
     * <p>If a customer with the given ID exists, it is returned.
     * If not, a new customer record is created and saved.
     *
     * <p>This method is useful in loosely-coupled systems where
     * the Order Service maintains a lightweight customer table
     * synced via authentication headers.
     *
     * @param customerId the unique UUID of the customer
     * @param name the customer name
     * @return the existing or newly created {@link Customer} entity
     */
    public Customer ensureCustomerExists(UUID customerId, String name) {
        log.debug("ensureCustomerExists customerId: {}", customerId);
        return customerRepository.findById(customerId)
                .orElseGet(() -> {
                    log.info("Creating new customer: {}", customerId);
                    return customerRepository.save(new Customer(customerId));
                });
    }



    /**
     * Verifies that a customer exists and is valid.
     *
     * <p>Validation steps:
     * <ol>
     *     <li>Checks that customerId is not null</li>
     *     <li>Ensures the customer exists in the database</li>
     *     <li>Optionally verifies name consistency (currently non-strict)</li>
     * </ol>
     *
     * <p>If validation fails:
     * <ul>
     *     <li>Throws {@link RuntimeException} if customerId is null</li>
     *     <li>Throws {@link CustomerNotFoundException} if customer not found</li>
     * </ul>
     *
     * <p>Name mismatch logic is currently relaxed (ID is trusted as primary identity).
     * You may enforce strict validation by throwing
     * {@link InvalidCustomerCredentialsException}.
     *
     * @param customerId the unique UUID of the customer
     * @param name the provided customer name (optional validation field)
     * @return the validated {@link Customer} entity
     * @throws RuntimeException if customerId is null
     * @throws CustomerNotFoundException if no customer exists with the given ID
     */
    public Customer verifyOrThrow(UUID customerId, String name) {
        if (customerId == null) {
            log.debug("verifyOrThrow rejected: null customerId");
            throw new InvalidCustomerCredentialsException(ErrorMessages.CUSTOMER_ID_REQUIRED_VERIFY);
        }

        log.debug("Verifying customer: {}", customerId);
        return customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("Customer not found for verification: {}", customerId);
                    return new CustomerNotFoundException(
                            Constants.CUSTOMER_NOT_FOUND_MESSAGE);
                });
    }
}
