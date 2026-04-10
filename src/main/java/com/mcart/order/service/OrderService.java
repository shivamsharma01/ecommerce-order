package com.mcart.order.service;

import com.mcart.order.dto.*;
import com.mcart.order.entity.OrderEntity;
import com.mcart.order.entity.OrderItemEntity;
import com.mcart.order.exception.CheckoutFailedException;
import com.mcart.order.repository.OrderItemRepository;
import com.mcart.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DownstreamClients downstream;

    @Transactional
    public CheckoutResponse checkout(UUID userId, String bearerToken) {
        CartResponse cart = downstream.getCart(bearerToken);
        if (cart == null || cart.items() == null || cart.items().isEmpty()) {
            throw new CheckoutFailedException("Cart is empty");
        }

        // Compute totals from current product prices (public endpoint)
        Map<String, ProductResponse> products = new HashMap<>();
        List<OrderItemResponse> items = new ArrayList<>();
        long total = 0L;

        for (CartItemResponse ci : cart.items()) {
            if (ci.quantity() <= 0) continue;
            ProductResponse p = products.computeIfAbsent(ci.productId(), downstream::getProduct);
            if (p == null) throw new CheckoutFailedException("Product not found: " + ci.productId());

            long unitPriceMinor = Math.round(p.price() * 100.0); // INR -> paise for consistency
            long lineTotal = unitPriceMinor * ci.quantity();
            total += lineTotal;
            items.add(new OrderItemResponse(ci.productId(), ci.quantity(), unitPriceMinor, lineTotal));
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
            downstream.incrementInventory(bearerToken, adjust);
            throw new CheckoutFailedException("Payment failed");
        }

        if (payment == null || !"SUCCESS".equalsIgnoreCase(payment.status())) {
            downstream.incrementInventory(bearerToken, adjust);
            throw new CheckoutFailedException("Payment failed");
        }

        // 3) persist order + items
        OrderEntity order = new OrderEntity();
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setStatus("PAID");
        order.setCreatedAt(Instant.now());
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

        return new CheckoutResponse(saved.getOrderId().toString(), saved.getStatus(), saved.getTotalAmount(), items);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listOrders(UUID userId) {
        List<OrderEntity> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<OrderSummaryResponse> out = new ArrayList<>();
        for (OrderEntity o : orders) {
            List<OrderItemResponse> items = orderItemRepository.findByOrderId(o.getOrderId()).stream()
                    .map(i -> new OrderItemResponse(i.getProductId(), i.getQuantity(), i.getUnitPrice(), i.getLineTotal()))
                    .toList();
            out.add(new OrderSummaryResponse(o.getOrderId().toString(), o.getStatus(), o.getTotalAmount(), o.getCreatedAt(), items));
        }
        return out;
    }
}

