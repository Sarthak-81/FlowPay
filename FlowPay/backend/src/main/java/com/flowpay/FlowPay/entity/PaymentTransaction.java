package com.flowpay.FlowPay.entity;

import java.time.LocalDateTime;

import com.flowpay.FlowPay.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing an individual payment transaction record.
 *
 * <p>A {@code PaymentTransaction} is created alongside each {@link Order} to
 * provide a detailed audit trail of payment events. It is updated by both the
 * manual verification flow ({@code POST /api/orders/verify}) and the Razorpay
 * webhook handler ({@code POST /api/payments/webhook}).</p>
 *
 * <p>Maps to the {@code payment_transactions} table in the database.</p>
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Internal order ID (string representation of {@link Order#getId()}).
     * Stored as a string to decouple the transaction log from the orders table.
     */
    @Column(name = "order_id")
    private String orderId;

    /**
     * Razorpay Order ID (e.g. {@code order_XXXXXXXXXX}).
     * Used to correlate with Razorpay's own records and webhook payloads.
     */
    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    /**
     * Razorpay Payment ID assigned after the user completes checkout
     * (e.g. {@code pay_XXXXXXXXXX}). Populated via webhook or verification.
     */
    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    /** Payment amount in rupees (INR). */
    private Double amount;

    /**
     * Current payment status.
     * Stored as a string in the database for readability.
     *
     * @see PaymentStatus
     */
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    /**
     * The Razorpay webhook event name that last updated this record
     * (e.g. {@code payment.captured}, {@code payment.failed}).
     */
    @Column(name = "webhook_event")
    private String webhookEvent;

    /** Timestamp when this transaction record was first created. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Timestamp of the most recent update to this transaction record. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
