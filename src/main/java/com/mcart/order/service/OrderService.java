package com.mcart.order.service;

import com.mcart.order.dto.*;
import com.mcart.order.entity.OrderEntity;
import com.mcart.order.entity.OrderItemEntity;
import com.mcart.order.exception.AddressRequiredException;
import com.mcart.order.exception.CheckoutFailedException;
import com.mcart.order.repository.OrderItemRepository;
import com.mcart.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DownstreamClients downstream;
    private final ObjectProvider<OrderPaidPublisher> orderPaidPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public CheckoutResponse checkout(UUID userId, String bearerToken, CheckoutRequest req) {
        SelectedAddress selected = resolveShippingAddress(userId, bearerToken, req);
        CartResponse cart = downstream.getCart(bearerToken);
        if (cart == null || cart.items() == null || cart.items().isEmpty()) {
            throw new CheckoutFailedException("Cart is empty");
        }
        log.info("Checkout started userId={} cartLineCount={}", userId, cart.items().size());

        // Compute totals from current product prices (public endpoint)
        Map<String, ProductResponse> products = new HashMap<>();
        List<OrderItemResponse> items = new ArrayList<>();
        long total = 0L;

        for (CartItemResponse ci : cart.items()) {
            if (ci.quantity() <= 0) continue;
            ProductResponse p = products.computeIfAbsent(ci.productId(), downstream::getProduct);
            if (p == null) throw new CheckoutFailedException("Product not found: " + ci.productId());

            long unitPriceMinor = Math.round(priceOrZero(p) * 100.0); // INR -> paise for consistency
            long lineTotal = unitPriceMinor * ci.quantity();
            total += lineTotal;
            items.add(new OrderItemResponse(
                    ci.productId(),
                    ci.quantity(),
                    unitPriceMinor,
                    lineTotal,
                    p.name(),
                    firstThumbnail(p)));
        }

        if (items.isEmpty() || total <= 0) {
            throw new CheckoutFailedException("Cart has no purchasable items");
        }

        String orderIdForDownstream = UUID.randomUUID().toString();
        InventoryAdjustRequest adjust = new InventoryAdjustRequest(
                orderIdForDownstream,
                items.stream().map(i -> new InventoryAdjustmentItem(i.productId(), i.quantity())).toList()
        );

        // 1) decrement inventory (throws on conflict)
        downstream.decrementInventory(bearerToken, adjust);

        // 2) charge (mock)
        ChargeResponse payment = null;
        try {
            payment = downstream.charge(bearerToken, new ChargeRequest(orderIdForDownstream, total));
        } catch (RuntimeException ex) {
            // treat downstream as payment failure; rollback stock
            log.warn("Payment downstream error after inventory decrement; rolling back stock userId={} ref={}",
                    userId, orderIdForDownstream);
            downstream.incrementInventory(bearerToken, adjust);
            throw new CheckoutFailedException("Payment failed");
        }

        if (payment == null || !"SUCCESS".equalsIgnoreCase(payment.status())) {
            log.warn("Payment not successful; rolling back stock userId={} ref={} status={}",
                    userId, orderIdForDownstream, payment != null ? payment.status() : "null");
            downstream.incrementInventory(bearerToken, adjust);
            throw new CheckoutFailedException("Payment failed");
        }

        final String paymentId = payment.paymentId();

        // 3) persist order + items
        OrderEntity order = new OrderEntity();
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setStatus("PAID");
        order.setCreatedAt(Instant.now());
        order.setShippingAddressId(selected.addressId);
        order.setShippingAddressJson(selected.addressJson);
        OrderEntity saved = orderRepository.save(order);

        for (OrderItemResponse it : items) {
            OrderItemEntity oi = new OrderItemEntity();
            oi.setOrderId(saved.getOrderId());
            oi.setProductId(it.productId());
            oi.setQuantity(it.quantity());
            oi.setUnitPrice(it.unitPrice());
            oi.setLineTotal(it.lineTotal());
            orderItemRepository.save(oi);
        }

        // 4) clear cart
        downstream.clearCart(bearerToken);

        log.info("Checkout completed userId={} orderId={} lineCount={} totalMinor={} paymentId={}",
                userId, saved.getOrderId(), items.size(), saved.getTotalAmount(), paymentId);

        Optional<String> emailOpt = downstream.getCustomerEmail(bearerToken);
        if (emailOpt.isEmpty()) {
            log.warn("No customer email for order {} (user profile missing or unverified); receipt event may omit email", saved.getOrderId());
        }
        orderPaidPublisher.ifAvailable(pub -> pub.publish(
                saved.getOrderId(),
                userId,
                orderIdForDownstream,
                paymentId,
                emailOpt.orElse(null),
                saved.getTotalAmount(),
                items
        ));

        return new CheckoutResponse(
                saved.getOrderId().toString(),
                saved.getStatus(),
                saved.getTotalAmount(),
                selected.address,
                items
        );
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listOrders(UUID userId) {
        List<OrderEntity> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        log.debug("Listing orders userId={} orderCount={}", userId, orders.size());
        List<OrderSummaryResponse> out = new ArrayList<>();
        for (OrderEntity o : orders) {
            List<OrderItemResponse> items = orderItemRepository.findByOrderId(o.getOrderId()).stream()
                    .map(i -> new OrderItemResponse(
                            i.getProductId(), i.getQuantity(), i.getUnitPrice(), i.getLineTotal(), null, null))
                    .toList();
            out.add(new OrderSummaryResponse(
                    o.getOrderId().toString(),
                    o.getStatus(),
                    o.getTotalAmount(),
                    o.getCreatedAt(),
                    decodeShippingAddress(o.getShippingAddressJson()),
                    enrichOrderLines(items)));
        }
        return out;
    }

    private List<OrderItemResponse> enrichOrderLines(List<OrderItemResponse> lines) {
        if (lines.isEmpty()) {
            return lines;
        }
        Map<String, ProductResponse> cache = new HashMap<>();
        List<OrderItemResponse> out = new ArrayList<>(lines.size());
        for (OrderItemResponse it : lines) {
            ProductResponse p = cache.computeIfAbsent(it.productId(), downstream::getProduct);
            if (p != null) {
                out.add(new OrderItemResponse(
                        it.productId(),
                        it.quantity(),
                        it.unitPrice(),
                        it.lineTotal(),
                        p.name(),
                        firstThumbnail(p)));
            } else {
                log.debug("Order line enrichment skipped (product unavailable) productId={}", it.productId());
                out.add(new OrderItemResponse(
                        it.productId(), it.quantity(), it.unitPrice(), it.lineTotal(), null, null));
            }
        }
        return out;
    }

    private static double priceOrZero(ProductResponse p) {
        return p.price() != null ? p.price() : 0.0;
    }

    private static String firstThumbnail(ProductResponse p) {
        if (p.gallery() == null || p.gallery().isEmpty()) {
            return null;
        }
        String u = p.gallery().getFirst().thumbnailUrl();
        return u != null && !u.isBlank() ? u : null;
    }

    private SelectedAddress resolveShippingAddress(UUID userId, String bearerToken, CheckoutRequest req) {
        UUID addressId = null;
        if (req != null && req.addressId() != null && !req.addressId().isBlank()) {
            try {
                addressId = UUID.fromString(req.addressId().trim());
            } catch (IllegalArgumentException ex) {
                throw new CheckoutFailedException("Invalid addressId");
            }
        }

        Optional<UserAddressResponse> addressOpt = addressId != null
                ? downstream.getAddressById(bearerToken, addressId)
                : downstream.getDefaultAddress(bearerToken);

        if (addressOpt.isEmpty()) {
            throw new AddressRequiredException("Please add a delivery address to continue.");
        }

        UserAddressResponse a = addressOpt.get();
        ShippingAddress shipping = new ShippingAddress(
                a.fullName(),
                a.phone(),
                a.line1(),
                a.line2(),
                a.city(),
                a.state(),
                a.pincode(),
                a.country()
        );

        try {
            String json = objectMapper.writeValueAsString(shipping);
            return new SelectedAddress(a.addressId(), shipping, json);
        } catch (Exception ex) {
            log.error("Failed to serialize shipping address for userId={}", userId, ex);
            throw new CheckoutFailedException("Could not save delivery address");
        }
    }

    private ShippingAddress decodeShippingAddress(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, ShippingAddress.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private record SelectedAddress(UUID addressId, ShippingAddress address, String addressJson) {}
}

