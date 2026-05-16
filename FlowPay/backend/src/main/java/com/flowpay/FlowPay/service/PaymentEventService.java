package com.flowpay.FlowPay.service;

import com.flowpay.FlowPay.entity.PaymentEvent;
import com.flowpay.FlowPay.entity.PaymentTransaction;
import com.flowpay.FlowPay.repository.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for recording {@link PaymentEvent} audit entries.
 *
 * <p>A {@link PaymentEvent} is an immutable record of a single state transition
 * for a {@link PaymentTransaction}. Every time a transaction's status changes
 * (e.g. {@code CREATED → SUCCESS}), a new event entry is written to the
 * {@code payment_events} table. This gives a complete, append-only history of
 * what happened to a payment and when.</p>
 *
 * <p>Events are recorded by:</p>
 * <ul>
 *   <li>{@code OrderService.createOrder}      – on initial transaction creation.</li>
 *   <li>{@code PaymentService.verifyPayment}  – when signature verification succeeds.</li>
 *   <li>{@code PaymentService.processWebhook} – when a Razorpay webhook is processed.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PaymentEventService {

    private final PaymentEventRepository paymentEventRepository;

    /**
     * Persists a new {@link PaymentEvent} representing a status transition.
     *
     * <p>All fields are written as supplied; no validation or deduplication is
     * performed here. Callers are responsible for passing correct values.</p>
     *
     * @param transaction the {@link PaymentTransaction} whose status has changed;
     *                    used to link the event to the transaction and its Razorpay Order ID
     * @param eventType   a short descriptor for what caused the transition
     *                    (e.g. {@code "PAYMENT_CREATED"}, {@code "PAYMENT_VERIFIED"},
     *                    {@code "PAYMENT_SUCCESS"}, {@code "PAYMENT_FAILED"})
     * @param oldStatus   the status before the transition (e.g. {@code "CREATED"});
     *                    pass {@code null} for the initial creation event
     * @param newStatus   the status after the transition (e.g. {@code "SUCCESS"})
     * @param message     a human-readable description of the event for debugging
     *                    (e.g. {@code "Payment captured via Razorpay webhook"})
     */
    public void recordEvent(
            PaymentTransaction transaction,
            String eventType,
            String oldStatus,
            String newStatus,
            String message) {

        PaymentEvent event = PaymentEvent.builder()
                .paymentTransactionId(transaction.getId())
                .razorpayOrderId(transaction.getRazorpayOrderId())
                .eventType(eventType)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        paymentEventRepository.save(event);
    }
}
