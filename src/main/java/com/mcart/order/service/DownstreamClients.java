package com.mcart.order.service;

import com.mcart.order.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DownstreamClients {

    private final RestClient rest = RestClient.create();

    @Value("${order.cart.base-url}")
    private String cartBaseUrl;

    @Value("${order.inventory.base-url}")
    private String inventoryBaseUrl;

    @Value("${order.payment.base-url}")
    private String paymentBaseUrl;

    @Value("${order.product.base-url}")
    private String productBaseUrl;

    @Value("${order.user.base-url}")
    private String userBaseUrl;

    public CartResponse getCart(String bearerToken) {
        return rest.get()
                .uri(cartBaseUrl + "/cart")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(CartResponse.class);
    }

    public void clearCart(String bearerToken) {
        rest.post()
                .uri(cartBaseUrl + "/cart/clear")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .toBodilessEntity();
    }

    public ProductResponse getProduct(String productId) {
        try {
            return rest.get()
                    .uri(productBaseUrl + "/api/products/" + productId)
                    .retrieve()
                    .body(ProductResponse.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public void decrementInventory(String bearerToken, InventoryAdjustRequest req) {
        rest.post()
                .uri(inventoryBaseUrl + "/inventory/decrement")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .body(req)
                .retrieve()
                .toBodilessEntity();
    }

    public void incrementInventory(String bearerToken, InventoryAdjustRequest req) {
        rest.post()
                .uri(inventoryBaseUrl + "/inventory/increment")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .body(req)
                .retrieve()
                .toBodilessEntity();
    }

    public ChargeResponse charge(String bearerToken, ChargeRequest req) {
        ResponseEntity<ChargeResponse> r = rest.post()
                .uri(paymentBaseUrl + "/payments/charge")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .body(req)
                .retrieve()
                .toEntity(ChargeResponse.class);
        return r.getBody();
    }

    public Optional<String> getCustomerEmail(String bearerToken) {
        try {
            UserMeResponse body = rest.get()
                    .uri(userBaseUrl + "/user/me")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(UserMeResponse.class);
            if (body == null || body.getEmail() == null || body.getEmail().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(body.getEmail().trim());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Optional<UserAddressResponse> getDefaultAddress(String bearerToken) {
        try {
            UserAddressResponse body = rest.get()
                    .uri(userBaseUrl + "/user/addresses/default")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(UserAddressResponse.class);
            return Optional.ofNullable(body);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Optional<UserAddressResponse> getAddressById(String bearerToken, UUID addressId) {
        try {
            UserAddressResponse body = rest.get()
                    .uri(userBaseUrl + "/user/addresses/" + addressId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(UserAddressResponse.class);
            return Optional.ofNullable(body);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public List<UserAddressResponse> listAddresses(String bearerToken) {
        try {
            UserAddressResponse[] body = rest.get()
                    .uri(userBaseUrl + "/user/addresses")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(UserAddressResponse[].class);
            return body != null ? List.of(body) : List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }
}

