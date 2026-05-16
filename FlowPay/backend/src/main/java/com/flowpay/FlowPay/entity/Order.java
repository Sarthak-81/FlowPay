package com.flowpay.FlowPay.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity representing a payment order in the FlowPay system.
 *
 * <p>An order is created when a user initiates a payment. It stores both the
 * internal order state and the Razorpay Order ID needed for the frontend checkout.</p>
 *
 * <p>Status lifecycle: {@code CREATED} → {@code PAID} | {@code FAILED}</p>
 *
 * <p>Maps to the {@code orders} table in the database.</p>
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Payment amount in rupees (INR). */
    private Double amount;

    /**
     * Current status of the order.
     * Possible values: {@code CREATED}, {@code PAID}, {@code FAILED}.
     */
    private String status;

    /**
     * Email address of the user who placed the order.
     * Used as a lightweight user reference (no FK constraint for now).
     */
    @Column(name = "user_email")
    private String userEmail;

    /** Timestamp when this order was first created. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * The Razorpay Order ID returned by the Razorpay API upon order creation.
     * Format: {@code order_XXXXXXXXXX}.
     * This value must be passed to the Razorpay checkout SDK on the frontend.
     */
    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}
