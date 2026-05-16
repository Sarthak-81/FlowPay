package com.flowpay.FlowPay.service;

import com.flowpay.FlowPay.dto.PaymentVerificationRequest;
import com.flowpay.FlowPay.dto.RazorpayWebhookRequest;
import com.flowpay.FlowPay.entity.Order;
import com.flowpay.FlowPay.entity.PaymentTransaction;
import com.flowpay.FlowPay.entity.WebhookEvent;
import com.flowpay.FlowPay.enums.PaymentStatus;
import com.flowpay.FlowPay.event.PaymentSuccessEvent;
import com.flowpay.FlowPay.exception.PaymentVerificationException;
import com.flowpay.FlowPay.exception.ResourceNotFoundException;
import com.flowpay.FlowPay.repository.OrderRepository;
import com.flowpay.FlowPay.repository.PaymentTransactionRepository;
import com.flowpay.FlowPay.repository.WebhookEventRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service layer for all Razorpay payment operations.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li><b>Order creation</b>: Calls the Razorpay Orders API to get a Razorpay Order ID.</li>
 *   <li><b>Payment verification</b>: Verifies the HMAC-SHA256 signature after client-side checkout.</li>
 *   <li><b>Webhook processing</b>: Handles {@code payment.captured} and {@code payment.failed}
 *       events sent by Razorpay.</li>
 * </ul>
 *
 * <p>Custom exceptions thrown here are mapped to HTTP status codes by
 * {@link com.flowpay.FlowPay.exception.GlobalExceptionHandler}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    @Value("${razorpay.key}")
    private String key;

    @Value("${razorpay.secret}")
    private String secret;

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentEventService paymentEventService;
    private final WebhookEventRepository webhookEventRepository;

    /**
     * Creates a Razorpay order via the Razorpay Orders API.
     *
     * <p>Converts the amount from rupees to paise (×100) as required by the
     * Razorpay API. The returned Razorpay Order ID must be stored on the
     * {@code Order} entity and sent to the frontend to open the checkout modal.</p>
     *
     * <p>The currency is hardcoded to {@code INR}. To support multi-currency,
     * accept a {@code currency} parameter and pass it through.</p>
     *
     * <h3>Razorpay API request (internal)</h3>
     * <pre>{@code
     * {
     *   "amount":   99900,       // paise (INR 999.00)
     *   "currency": "INR",
     *   "receipt":  "txn_<epoch>"
     * }
     * }</pre>
     *
     * @param amount the payment amount in rupees (INR)
     * @return the Razorpay Order ID string (e.g. {@code order_XXXXXXXXXX})
     * @throws RazorpayException if the Razorpay API returns an error
     *                           (invalid credentials, network failure, etc.)
     */
    public String createRazorpayOrder(Double amount) throws Exception {

        RazorpayClient client = new RazorpayClient(key, secret);

        JSONObject options = new JSONObject();
        // Razorpay expects the amount in the smallest currency unit (paise for INR).
        options.put("amount", (long) (amount * 100));
        options.put("currency", "INR");
        // A unique receipt identifier for internal tracking; not shown to the user.
        options.put("receipt", "txn_" + System.currentTimeMillis());

        com.razorpay.Order order = client.orders.create(options);

        return order.get("id").toString();
    }

    /**
     * Verifies a Razorpay payment signature after the frontend checkout completes.
     *
     * <p>Razorpay computes an HMAC-SHA256 over the string
     * {@code "<razorpay_order_id>|<razorpay_payment_id>"} using your Razorpay
     * secret key. We recompute the same hash and compare it to the
     * {@code razorpay_signature} supplied by the client. A mismatch means the
     * response was tampered with.</p>
     *
     * <p>On successful verification:</p>
     * <ol>
     *   <li>The {@code Order} status is updated to {@code PAID}.</li>
     *   <li>The {@code PaymentTransaction} is updated with the Razorpay Payment ID
     *       and status {@code SUCCESS}.</li>
     *   <li>A {@code PAYMENT_VERIFIED} audit event is recorded.</li>
     * </ol>
     *
     * <h3>Request body fields</h3>
     * <ul>
     *   <li>{@code razorpay_order_id}   – The order ID returned at order creation.</li>
     *   <li>{@code razorpay_payment_id} – The payment ID returned after checkout.</li>
     *   <li>{@code razorpay_signature}  – The HMAC-SHA256 hex string from Razorpay.</li>
     * </ul>
     *
     * @param request the verification DTO with the three Razorpay identifiers
     * @return a success confirmation string
     * @throws PaymentVerificationException if the signature does not match
     * @throws ResourceNotFoundException    if the order or transaction cannot be found
     * @throws Exception                    if the Razorpay SDK throws during verification
     */
    public String verifyPayment(PaymentVerificationRequest request) throws Exception {

        // Construct the payload exactly as Razorpay specifies: "order_id|payment_id"
        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();

        boolean isValid;
        try {
            isValid = Utils.verifySignature(payload, request.getRazorpaySignature(), secret);
        } catch (Exception e) {
            // Wrap SDK-level errors (e.g. algorithm not found) in our typed exception.
            throw new PaymentVerificationException(
                    "Signature verification could not be completed: " + e.getMessage(), e);
        }

        if (!isValid) {
            // Throw a typed exception so GlobalExceptionHandler returns 422, not a generic 500.
            throw new PaymentVerificationException(
                    "Invalid payment signature. The payment response may have been tampered with.");
        }

        Order order = orderRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found for Razorpay Order ID: " + request.getRazorpayOrderId()));

        order.setStatus("PAID");
        orderRepository.save(order);

        PaymentTransaction transaction = paymentTransactionRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment transaction not found for Razorpay Order ID: " + request.getRazorpayOrderId()));

        PaymentStatus oldStatus = transaction.getStatus();

        transaction.setRazorpayPaymentId(request.getRazorpayPaymentId());
        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setUpdatedAt(LocalDateTime.now());

        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

        paymentEventService.recordEvent(
                savedTransaction,
                "PAYMENT_VERIFIED",
                oldStatus != null ? oldStatus.name() : null,
                PaymentStatus.SUCCESS.name(),
                "Payment verified using Razorpay signature"
        );

        log.info("Payment verified. Order {} marked PAID. Transaction updated with payment ID {}",
                request.getRazorpayOrderId(), request.getRazorpayPaymentId());

        return "Payment verified successfully";
    }

    /**
     * Processes an incoming Razorpay webhook event.
     *
     * <p>Razorpay sends webhook POST requests to {@code /api/payments/webhook} after
     * key payment lifecycle events. This method:</p>
     * <ol>
     *   <li>Persists a {@link WebhookEvent} record immediately (before any processing)
     *       so no event is lost even if downstream steps fail.</li>
     *   <li>Looks up the matching {@link PaymentTransaction} by Razorpay Order ID.</li>
     *   <li>Updates the transaction status based on the event type:
     *       <ul>
     *         <li>{@code payment.captured} → {@code SUCCESS}; publishes a Kafka event.</li>
     *         <li>Any other event        → {@code FAILED}.</li>
     *       </ul>
     *   </li>
     *   <li>Marks the {@link WebhookEvent} as processed.</li>
     * </ol>
     *
     * <p><b>Note on signature verification:</b> The {@code X-Razorpay-Signature} header
     * is passed in but not yet verified here. For production, add HMAC-SHA256 verification
     * of the raw request body against this header using the Razorpay webhook secret.</p>
     *
     * <p><b>Fix:</b> The original code called {@code orElseThrow()} inside a {@code void}
     * method with a generic {@code RuntimeException}. This would propagate as an unhandled
     * 500 to Razorpay, causing it to retry indefinitely. The fix uses
     * {@link ResourceNotFoundException} (→ mapped to 404 by {@code GlobalExceptionHandler})
     * and logs the error before re-throwing, so Razorpay receives a non-500 response
     * and stops retrying.</p>
     *
     * @param request   the parsed webhook body containing {@code event} and {@code payload}
     * @param signature the HMAC-SHA256 value from the {@code X-Razorpay-Signature} header
     *                  (reserved for future signature verification)
     */
    public void processWebhook(RazorpayWebhookRequest request, String signature) {

        log.info("Webhook received: {}", request.getEvent());

        // Safely extract nested fields; null-check guards against malformed webhook bodies.
        if (request.getPayload() == null
                || request.getPayload().getPayment() == null
                || request.getPayload().getPayment().getEntity() == null) {

            log.error("Malformed webhook payload received for event '{}'. Ignoring.", request.getEvent());
            // Return without throwing so Razorpay receives 200 and does not retry this malformed event.
            return;
        }

        String paymentId = request.getPayload().getPayment().getEntity().getId();
        // Use getOrderId() — the field was renamed from order_id to orderId in RazorpayWebhookRequest.Entity
        String orderId   = request.getPayload().getPayment().getEntity().getOrderId();

        // Persist the raw webhook event first, before any business logic, so it is
        // never lost even if subsequent database operations fail.
        WebhookEvent webhookEvent = WebhookEvent.builder()
                .source("RAZORPAY")
                .eventType(request.getEvent())
                .razorpayOrderId(orderId)
                .razorpayPaymentId(paymentId)
                .signature(signature)
                .processed(false)
                .receivedAt(LocalDateTime.now())
                .build();

        WebhookEvent savedWebhook = webhookEventRepository.save(webhookEvent);

        PaymentTransaction transaction;
        try {
            transaction = paymentTransactionRepository
                    .findByRazorpayOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Payment transaction not found for Razorpay Order ID: " + orderId));
        } catch (ResourceNotFoundException e) {
            log.error("Webhook processing skipped — {}", e.getMessage());
            // Re-throw so GlobalExceptionHandler returns 404 to Razorpay.
            // Razorpay treats non-2xx responses as a delivery failure and will retry,
            // which is appropriate here since missing data may be a race condition.
            throw e;
        }

        PaymentStatus oldStatus = transaction.getStatus();

        transaction.setRazorpayPaymentId(paymentId);
        transaction.setWebhookEvent(request.getEvent());
        transaction.setUpdatedAt(LocalDateTime.now());

        if ("payment.captured".equals(request.getEvent())) {

            transaction.setStatus(PaymentStatus.SUCCESS);
            PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

            paymentEventService.recordEvent(
                    savedTransaction,
                    "PAYMENT_SUCCESS",
                    oldStatus != null ? oldStatus.name() : null,
                    PaymentStatus.SUCCESS.name(),
                    "Payment captured via Razorpay webhook"
            );

            // Publish to Kafka so downstream consumers (e.g. NotificationService) can react.
            PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                    .orderId(savedTransaction.getOrderId())
                    .razorpayOrderId(savedTransaction.getRazorpayOrderId())
                    .razorpayPaymentId(savedTransaction.getRazorpayPaymentId())
                    .amount(savedTransaction.getAmount())
                    .status(savedTransaction.getStatus().name())
                    .eventTime(LocalDateTime.now())
                    .build();

            paymentEventProducer.publishPaymentSuccess(event);

            savedWebhook.setProcessed(true);
            savedWebhook.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(savedWebhook);

            log.info("Payment captured successfully for order {}", orderId);

        } else {

            // Treat any non-captured event as a failed payment.
            transaction.setStatus(PaymentStatus.FAILED);
            PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

            paymentEventService.recordEvent(
                    savedTransaction,
                    "PAYMENT_FAILED",
                    oldStatus != null ? oldStatus.name() : null,
                    PaymentStatus.FAILED.name(),
                    "Payment failed via Razorpay webhook (event: " + request.getEvent() + ")"
            );

            savedWebhook.setProcessed(true);
            savedWebhook.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(savedWebhook);

            log.error("Payment failed for order {} (event: {})", orderId, request.getEvent());
        }
    }
}
