package com.flowpay.FlowPay.exception;

/**
 * Exception thrown when a requested resource cannot be found in the database.
 *
 * <p>Examples of when this is thrown:</p>
 * <ul>
 *   <li>Looking up an {@code Order} by Razorpay Order ID that doesn't exist.</li>
 *   <li>Looking up a {@code PaymentTransaction} for an unknown order.</li>
 *   <li>An idempotency key references an order ID that was deleted.</li>
 * </ul>
 *
 * <p>Handled by {@link GlobalExceptionHandler#handleResourceNotFound}, which
 * converts it to a <b>404 Not Found</b> HTTP response.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new exception with the given message.
     *
     * @param message a human-readable description of what was not found
     *                (e.g. {@code "Order not found: order_XXXXXXXXXX"})
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the given message and root cause.
     *
     * @param message a human-readable description of what was not found
     * @param cause   the underlying exception that triggered this one
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
