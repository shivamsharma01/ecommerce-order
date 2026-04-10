package com.mcart.order.controller;

import com.mcart.order.dto.CheckoutResponse;
import com.mcart.order.dto.OrderSummaryResponse;
import com.mcart.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String CLAIM_USER_ID = "userId";

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(CLAIM_USER_ID));
        CheckoutResponse r = orderService.checkout(userId, authorization);
        return ResponseEntity.ok(r);
    }

    @GetMapping
    public ResponseEntity<List<OrderSummaryResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(CLAIM_USER_ID));
        return ResponseEntity.ok(orderService.listOrders(userId));
    }
}
