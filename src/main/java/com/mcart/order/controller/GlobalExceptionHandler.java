package com.mcart.order.controller;

import com.mcart.order.dto.ApiError;
import com.mcart.order.exception.AddressRequiredException;
import com.mcart.order.exception.CheckoutFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AddressRequiredException.class)
    public ResponseEntity<ApiError> handleAddressRequired(AddressRequiredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("ADDRESS_REQUIRED", ex.getMessage()));
    }

    @ExceptionHandler(CheckoutFailedException.class)
    public ResponseEntity<String> handleCheckoutFailed(CheckoutFailedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}

