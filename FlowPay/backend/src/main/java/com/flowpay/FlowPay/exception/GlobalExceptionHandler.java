package com.flowpay.FlowPay.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the FlowPay application.
 *
 * <p>Annotated with {@code @RestControllerAdvice} so Spring automatically routes
 * any unhandled exception thrown by a {@code @RestController} through this class
 * before the response is written to the client. This prevents raw stack traces
 * from leaking and ensures every error response has a consistent JSON shape:</p>
 *
 * <pre>{@code
 * {
 *   "timestamp": "2025-01-01T10:00:00",
 *   "status":    400,
 *   "error":     "Bad Request",
 *   "message":   "Human-readable explanation",
 *   "path":      "/api/orders"
 * }
 * }</pre>
 *
 * <h3>Handler precedence</h3>
 * Spring matches the most-specific handler first, so concrete exception types
 * ({@link ResourceNotFoundException}, {@link IllegalArgumentException}, etc.) take
 * precedence over the catch-all {@link Exception} handler at the bottom.
 *
 * <h3>Logging strategy</h3>
 * <ul>
 *   <li><b>WARN</b>  – client errors (4xx): the caller did something wrong; no action required by us.</li>
 *   <li><b>ERROR</b> – server errors (5xx): something unexpected failed; requires investigation.</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // Custom domain exceptions
    // -------------------------------------------------------------------------

    /**
     * Handles {@link ResourceNotFoundException} — thrown when a requested resource
     * (order, user, transaction) does not exist in the database.
     *
     * <p>Returns <b>404 Not Found</b>.</p>
     *
     * @param ex      the exception carrying the not-found message
     * @param request the current HTTP request (used to populate the {@code path} field)
     * @return 404 error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handles {@link DuplicateResourceException} — thrown when a creation request
     * conflicts with an existing record (e.g. duplicate email on signup).
     *
     * <p>Returns <b>409 Conflict</b>.</p>
     *
     * @param ex      the exception carrying the conflict message
     * @param request the current HTTP request
     * @return 409 error response
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {

        log.warn("Duplicate resource conflict [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handles {@link PaymentVerificationException} — thrown when Razorpay signature
     * verification fails, indicating a tampered or replayed payment response.
     *
     * <p>Returns <b>422 Unprocessable Entity</b> to signal that the request was
     * well-formed but semantically invalid (the signature did not match).</p>
     *
     * @param ex      the exception carrying the verification failure reason
     * @param request the current HTTP request
     * @return 422 error response
     */
    @ExceptionHandler(PaymentVerificationException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentVerification(
            PaymentVerificationException ex, HttpServletRequest request) {

        log.warn("Payment verification failed [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI());
    }

    // -------------------------------------------------------------------------
    // Authentication & JWT exceptions
    // -------------------------------------------------------------------------

    /**
     * Handles {@link ExpiredJwtException} — thrown by {@code JwtUtil} when the
     * token's expiry timestamp is in the past.
     *
     * <p>Returns <b>401 Unauthorized</b> with a clear message prompting the
     * client to re-authenticate.</p>
     *
     * @param ex      the expired-token exception (contains the expired claims)
     * @param request the current HTTP request
     * @return 401 error response
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Map<String, Object>> handleExpiredJwt(
            ExpiredJwtException ex, HttpServletRequest request) {

        log.warn("JWT expired [{}]", request.getRequestURI());
        return buildResponse(HttpStatus.UNAUTHORIZED,
                "Your session has expired. Please log in again.", request.getRequestURI());
    }

    /**
     * Handles all other {@link JwtException} subtypes (malformed token, bad
     * signature, unsupported algorithm, etc.).
     *
     * <p>Returns <b>401 Unauthorized</b>.</p>
     *
     * @param ex      the JWT exception
     * @param request the current HTTP request
     * @return 401 error response
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, Object>> handleJwt(
            JwtException ex, HttpServletRequest request) {

        log.warn("Invalid JWT [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED,
                "Invalid or malformed authentication token.", request.getRequestURI());
    }

    /**
     * Handles {@link MissingRequestHeaderException} — thrown when a required header
     * (e.g. {@code Authorization} or {@code X-Razorpay-Signature}) is absent.
     *
     * <p>Returns <b>400 Bad Request</b> with the name of the missing header.</p>
     *
     * @param ex      the exception identifying the missing header name
     * @param request the current HTTP request
     * @return 400 error response
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {

        log.warn("Missing required header '{}' [{}]", ex.getHeaderName(), request.getRequestURI());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Required header is missing: " + ex.getHeaderName(), request.getRequestURI());
    }

    // -------------------------------------------------------------------------
    // Validation & request-format exceptions
    // -------------------------------------------------------------------------

    /**
     * Handles Bean Validation failures ({@code @Valid} on request bodies or
     * method parameters). Collects all field-level error messages into a list
     * so the client knows exactly which fields are invalid.
     *
     * <p>Returns <b>400 Bad Request</b> with an {@code "errors"} array.</p>
     *
     * <p>Example response body:</p>
     * <pre>{@code
     * {
     *   "message": "Validation failed",
     *   "errors": ["email: must not be blank", "amount: must be positive"]
     * }
     * }</pre>
     *
     * @param ex      the validation exception holding field-level binding results
     * @param request the current HTTP request
     * @return 400 error response with per-field messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        log.warn("Validation failed [{}]: {}", request.getRequestURI(), errors);

        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI());
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles {@link HttpMessageNotReadableException} — thrown when the request body
     * is missing, malformed JSON, or cannot be deserialized into the target type.
     *
     * <p>Returns <b>400 Bad Request</b>.</p>
     *
     * @param ex      the message-not-readable exception
     * @param request the current HTTP request
     * @return 400 error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Malformed request body [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Request body is missing or contains invalid JSON.", request.getRequestURI());
    }

    /**
     * Handles {@link MethodArgumentTypeMismatchException} — thrown when a path
     * variable or query parameter cannot be converted to the expected type
     * (e.g. passing {@code "abc"} for a {@code Long} parameter).
     *
     * <p>Returns <b>400 Bad Request</b>.</p>
     *
     * @param ex      the type-mismatch exception
     * @param request the current HTTP request
     * @return 400 error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message = String.format("Parameter '%s' has invalid value '%s'.",
                ex.getName(), ex.getValue());
        log.warn("Type mismatch [{}]: {}", request.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    /**
     * Handles {@link IllegalArgumentException} — thrown for general business-logic
     * rule violations (e.g. negative order amount, empty item list).
     *
     * <p>Returns <b>400 Bad Request</b>.</p>
     *
     * @param ex      the exception carrying the violation message
     * @param request the current HTTP request
     * @return 400 error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Illegal argument [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    // -------------------------------------------------------------------------
    // HTTP-level exceptions
    // -------------------------------------------------------------------------

    /**
     * Handles {@link NoHandlerFoundException} — thrown when no controller mapping
     * matches the incoming URL (i.e. the route does not exist).
     *
     * <p>Returns <b>404 Not Found</b>.</p>
     *
     * <p><b>Note:</b> Requires {@code spring.mvc.throw-exception-if-no-handler-found=true}
     * and {@code spring.web.resources.add-mappings=false} in {@code application.yaml}
     * for this handler to fire instead of Spring's default 404 page.</p>
     *
     * @param ex      the no-handler exception
     * @param request the current HTTP request
     * @return 404 error response
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandler(
            NoHandlerFoundException ex, HttpServletRequest request) {

        log.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return buildResponse(HttpStatus.NOT_FOUND,
                "The requested endpoint does not exist: " + ex.getRequestURL(), request.getRequestURI());
    }

    // -------------------------------------------------------------------------
    // Database exceptions
    // -------------------------------------------------------------------------

    /**
     * Handles {@link DataIntegrityViolationException} — thrown by JPA/Hibernate
     * when a database constraint is violated (e.g. a {@code UNIQUE} constraint
     * on the {@code users.email} column).
     *
     * <p>Returns <b>409 Conflict</b>. The root cause message is logged server-side
     * but only a generic message is returned to the client to avoid leaking
     * schema details.</p>
     *
     * @param ex      the data-integrity exception
     * @param request the current HTTP request
     * @return 409 error response
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.error("Data integrity violation [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT,
                "A record with the same unique identifier already exists.", request.getRequestURI());
    }

    // -------------------------------------------------------------------------
    // Catch-all fallback
    // -------------------------------------------------------------------------

    /**
     * Catch-all handler for any {@link Exception} not matched by a more specific
     * handler above.
     *
     * <p>Returns <b>500 Internal Server Error</b>. The full stack trace is logged
     * at ERROR level so engineers can investigate, but only a generic message is
     * returned to the client to avoid leaking implementation details.</p>
     *
     * @param ex      the unexpected exception
     * @param request the current HTTP request
     * @return 500 error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", request.getRequestURI());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a standardized error {@link ResponseEntity}.
     *
     * @param status  the HTTP status to use
     * @param message the human-readable error message
     * @param path    the request path that triggered the error
     * @return a {@link ResponseEntity} wrapping the error map
     */
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(buildBody(status, message, path));
    }

    /**
     * Constructs the error response map with a consistent structure.
     *
     * @param status  the HTTP status
     * @param message the error description
     * @param path    the originating request URI
     * @return an ordered map suitable for JSON serialization
     */
    private Map<String, Object> buildBody(HttpStatus status, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return body;
    }
}
