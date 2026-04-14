package com.mcart.order.exception;

public class AddressRequiredException extends RuntimeException {
    public AddressRequiredException(String message) {
        super(message);
    }
}

