package com.foodapp.order_service.exception;

public class InvalidCustomerCredentialsException extends RuntimeException{
    public InvalidCustomerCredentialsException(String message){
        super(message);
    }
}
