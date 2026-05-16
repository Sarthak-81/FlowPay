package com.flowpay.FlowPay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for deserializing the Razorpay webhook request body.
 *
 * <p>Razorpay sends webhook POST requests to {@code /api/payments/webhook} after
 * payment lifecycle events. The JSON body follows this structure:</p>
 *
 * <pre>{@code
 * {
 *   "event": "payment.captured",
 *   "payload": {
 *     "payment": {
 *       "entity": {
 *         "id":       "pay_XXXXXXXXXX",
 *         "order_id": "order_XXXXXXXXXX",
 *         "status":   "captured",
 *         "amount":   99900
 *       }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>Key event types handled by {@link com.flowpay.FlowPay.service.PaymentService#processWebhook}:</p>
 * <ul>
 *   <li>{@code payment.captured} — payment was successful; update status to {@code SUCCESS}.</li>
 *   <li>{@code payment.failed}   — payment failed; update status to {@code FAILED}.</li>
 * </ul>
 *
 * <p><b>Security note:</b> Razorpay signs each webhook with an HMAC-SHA256 over the raw
 * request body using a webhook secret. The {@code X-Razorpay-Signature} header should
 * be verified before trusting this payload.</p>
 */
@Data
public class RazorpayWebhookRequest {

    /** The name of the Razorpay event (e.g. {@code payment.captured}, {@code payment.failed}). */
    private String event;

    /**
     * The webhook payload containing the payment entity details.
     * Mapped from the {@code payload} JSON key.
     */
    private Payload payload;

    /**
     * Top-level payload wrapper.
     *
     * <p>Razorpay wraps the entity in an extra {@code payment} object,
     * so the path to the entity is {@code payload.payment.entity}.</p>
     */
    @Data
    public static class Payload {

        /** The payment wrapper containing the entity. */
        private PaymentEntity payment;
    }

    /**
     * Wrapper around the actual payment entity returned by Razorpay.
     *
     * <p>Razorpay uses this extra nesting layer to allow a single webhook
     * payload format to support multiple entity types in future.</p>
     */
    @Data
    public static class PaymentEntity {

        /** The actual Razorpay payment details. */
        private Entity entity;
    }

    /**
     * The Razorpay payment entity with the fields relevant to FlowPay.
     *
     * <p><b>Fix:</b> The original code declared {@code order_id} with both a field name
     * of {@code order_id} and a {@code @JsonProperty("order_id")} annotation, which is
     * redundant and misleading. The field has been renamed to {@code orderId} (Java
     * naming convention) while keeping the {@code @JsonProperty} annotation to map the
     * snake_case JSON key correctly.</p>
     */
    @Data
    public static class Entity {

        /**
         * The Razorpay Payment ID (e.g. {@code pay_XXXXXXXXXX}).
         * Assigned by Razorpay after the user completes checkout.
         */
        private String id;

        /**
         * The Razorpay Order ID (e.g. {@code order_XXXXXXXXXX}).
         * Links this payment back to the order that was created.
         *
         * <p><b>Fix:</b> Previously the field was named {@code order_id} (violating Java
         * naming conventions). It is now {@code orderId} with an explicit {@code @JsonProperty}
         * to handle the snake_case JSON key from Razorpay.</p>
         */
        @JsonProperty("order_id")
        private String orderId;

        /**
         * The payment status as reported by Razorpay
         * (e.g. {@code captured}, {@code failed}, {@code authorized}).
         */
        private String status;

        /**
         * The payment amount in paise (the smallest INR unit).
         * Divide by 100 to get rupees. Razorpay always returns this in paise.
         */
        private Integer amount;
    }
}
