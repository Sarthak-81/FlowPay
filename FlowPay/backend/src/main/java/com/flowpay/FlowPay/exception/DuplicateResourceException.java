package com.flowpay.FlowPay.exception;

/**
 * Exception thrown when a creation or update request would violate a uniqueness constraint.
 *
 * <p>Currently thrown in:</p>
 * <ul>
 *   <li>{@code AuthService.signup} — when the provided email is already registered.</li>
 * </ul>
 *
 * <p>Handled by {@link GlobalExceptionHandler#handleDuplicateResource}, which
 * converts it to a <b>409 Conflict</b> HTTP response.</p>
 */
public class DuplicateResourceException extends RuntimeException {

    /**
     * Constructs a new exception with the given message.
     *
     * @param message a human-readable description of the conflict
     *                (e.g. {@code "Email already exists: user@example.com"})
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}
