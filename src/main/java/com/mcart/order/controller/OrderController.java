package com.mcart.order.controller;

import com.mcart.order.dto.CheckoutResponse;
import com.mcart.order.dto.OrderSummaryResponse;
import com.mcart.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.mcart.order.config.OpenApiConfig.BEARER_JWT;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private static final String CLAIM_USER_ID = "userId";

    private final OrderService orderService;

    @PostMapping("/checkout")
    @Operation(summary = "Checkout current cart into an order")
    @SecurityRequirement(name = BEARER_JWT)
    public ResponseEntity<CheckoutResponse> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(CLAIM_USER_ID));
        CheckoutResponse r = orderService.checkout(userId, authorization);
        return ResponseEntity.ok(r);
    }

    @GetMapping
    @Operation(summary = "List current user's orders")
    @SecurityRequirement(name = BEARER_JWT)
    public ResponseEntity<List<OrderSummaryResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(CLAIM_USER_ID));
        return ResponseEntity.ok(orderService.listOrders(userId));
    }
}

