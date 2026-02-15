package com.foodapp.delivery_service.exception;

import java.util.UUID;

public class DeliveryNotFoundException extends RuntimeException{
    public DeliveryNotFoundException(UUID orderId){
        super("Delivery not found for orderId= "+orderId);
    }

}
