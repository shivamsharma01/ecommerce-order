package com.mcart.order.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "shipping_address_id")
    private UUID shippingAddressId;

    @Column(name = "shipping_address_json", columnDefinition = "jsonb")
    private String shippingAddressJson;

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getShippingAddressId() {
        return shippingAddressId;
    }

    public void setShippingAddressId(UUID shippingAddressId) {
        this.shippingAddressId = shippingAddressId;
    }

    public String getShippingAddressJson() {
        return shippingAddressJson;
    }

    public void setShippingAddressJson(String shippingAddressJson) {
        this.shippingAddressJson = shippingAddressJson;
    }
}

