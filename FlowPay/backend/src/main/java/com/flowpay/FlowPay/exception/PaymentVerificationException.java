package com.flowpay.FlowPay.exception;

/**
 * Exception thrown when Razorpay payment signature verification fails.
 *
 * <p>Signature verification is performed in {@code PaymentService.verifyPayment}
 * by computing an HMAC-SHA256 over {@code razorpay_order_id|razorpay_payment_id}
 * using the Razorpay secret key, then comparing it to the {@code razorpay_signature}
 * supplied by the client. A mismatch indicates either a tampered response or an
 * incorrect secret key configuration.</p>
 *
 * <p>Handled by {@link GlobalExceptionHandler#handlePaymentVerification}, which
 * converts it to a <b>422 Unprocessable Entity</b> HTTP response.</p>
 */
public class PaymentVerificationException extends RuntimeException {

    /**
     * Constructs a new exception with the given message.
     *
     * @param message a human-readable description of why verification failed
     */
    public PaymentVerificationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the given message and root cause.
     *
     * @param message a human-readable description of why verification failed
     * @param cause   the underlying exception from the Razorpay SDK
     */
    public PaymentVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
