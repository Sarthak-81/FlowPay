package com.flowpay.FlowPay.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the FlowPay application.
 *
 * <p>Intercepts exceptions thrown by any {@code @RestController} and translates
 * them into appropriate HTTP responses, preventing raw stack traces from being
 * returned to the client.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all {@link RuntimeException} instances thrown within controller methods.
     *
     * <p>Returns a 400 Bad Request response with the exception message as the body.
     * This covers common cases such as "User not found", "Invalid password",
     * "Email already exists", and "Invalid payment signature".</p>
     *
     * @param ex the runtime exception that was thrown
     * @return a 400 response entity containing the error message
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }
}
