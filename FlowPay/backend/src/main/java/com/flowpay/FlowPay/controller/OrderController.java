package com.flowpay.FlowPay.controller;

import com.flowpay.FlowPay.dto.OrderRequest;
import com.flowpay.FlowPay.dto.PaymentVerificationRequest;
import com.flowpay.FlowPay.entity.Order;
import com.flowpay.FlowPay.service.OrderService;
import com.flowpay.FlowPay.service.PaymentService;
import com.flowpay.FlowPay.utility.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for order management and payment verification.
 *
 * <p>Exposes three endpoints under {@code /api/orders}:</p>
 * <ul>
 *   <li>{@code POST /api/orders}        – Creates a new order and a Razorpay order.</li>
 *   <li>{@code GET  /api/orders}        – Retrieves all orders for the authenticated user.</li>
 *   <li>{@code POST /api/orders/verify} – Verifies a Razorpay payment signature.</li>
 * </ul>
 *
 * <p>All endpoints require a valid JWT Bearer token (enforced by
 * {@link com.flowpay.FlowPay.config.JwtFilter}).</p>
 *
 * <h3>Authentication approach</h3>
 * <p>The user's identity (email) is obtained from the injected Spring Security
 * {@link Authentication} object, which is populated by {@code JwtFilter} on every
 * authenticated request. This is cleaner and safer than manually parsing the
 * {@code Authorization} header in the controller.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    /**
     * {@code JwtUtil} is injected here only because {@code OrderService.createOrder}
     * needs the user email extracted from the token.
     *
     * <p><b>Fix:</b> The original code referenced {@code jwtUtil} without declaring
     * it as a field, causing a compilation error. It is now properly injected via the
     * Lombok {@code @RequiredArgsConstructor} (the field is {@code final}).</p>
     *
     * <p><b>Note:</b> Prefer using the {@link Authentication} object (already populated
     * by {@code JwtFilter}) instead of re-parsing the token in the controller.
     * This controller now uses {@code Authentication} consistently across all endpoints.</p>
     */
    private final JwtUtil jwtUtil;

    /**
     * Creates a new order for the currently authenticated user.
     *
     * <p>This endpoint:</p>
     * <ol>
     *   <li>Persists an {@code Order} entity with status {@code CREATED}.</li>
     *   <li>Calls the Razorpay API to create a corresponding Razorpay order.</li>
     *   <li>Persists a {@code PaymentTransaction} record linked to the order.</li>
     *   <li>Returns the saved {@code Order} (which includes the {@code razorpayOrderId}
     *       the frontend needs to open the Razorpay checkout).</li>
     * </ol>
     *
     * <p><b>Idempotency:</b> If an {@code Idempotency-Key} header is provided and a
     * request with that key has already been processed, the original order is returned
     * without creating a duplicate. This prevents double-charges if the client retries
     * a timed-out request.</p>
     *
     * <p><b>Fix:</b> The original code manually extracted the email by calling
     * {@code jwtUtil.extractUsername(token)} — a method that does not exist (the correct
     * name is {@code extractEmail}). The controller now reads the email directly from
     * the Spring Security {@link Authentication} object, which is already populated by
     * {@code JwtFilter}, making the JWT utility call in the controller unnecessary.</p>
     *
     * <h3>Request</h3>
     * <pre>{@code
     * POST /api/orders
     * Authorization: Bearer <jwt>
     * Idempotency-Key: <uuid>   (optional)
     * Content-Type: application/json
     *
     * {
     *   "items": [
     *     { "itemName": "Widget", "quantity": 2, "unitPrice": 499.50 }
     *   ]
     * }
     * }</pre>
     *
     * <h3>Response (201 Created)</h3>
     * <pre>{@code
     * {
     *   "id": 1,
     *   "amount": 999.00,
     *   "status": "CREATED",
     *   "userEmail": "user@example.com",
     *   "razorpayOrderId": "order_XXXXXXXXXX",
     *   "createdAt": "2025-01-01T10:00:00"
     * }
     * }</pre>
     *
     * @param idempotencyKey optional client-generated key to prevent duplicate orders
     * @param request        the order body containing the list of items to purchase
     * @param auth           Spring Security authentication object;
     *                       {@code auth.getName()} returns the authenticated user's email
     * @return 201 Created with the persisted {@link Order} as JSON
     * @throws Exception if the Razorpay API call fails
     */
    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody OrderRequest request,
            Authentication auth) throws Exception {

        // auth.getName() is the email set by JwtFilter — no need to re-parse the JWT here.
        String email = auth.getName();
        Order order = orderService.createOrder(email, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Retrieves all orders belonging to the currently authenticated user.
     *
     * <h3>Request</h3>
     * <pre>{@code
     * GET /api/orders
     * Authorization: Bearer <jwt>
     * }</pre>
     *
     * <h3>Response (200 OK)</h3>
     * <pre>{@code
     * [
     *   {
     *     "id": 1,
     *     "amount": 999.00,
     *     "status": "PAID",
     *     "razorpayOrderId": "order_XXXXXXXXXX",
     *     "createdAt": "2025-01-01T10:00:00"
     *   }
     * ]
     * }</pre>
     *
     * @param auth Spring Security authentication; {@code auth.getName()} is the user's email
     * @return 200 OK with a (possibly empty) list of {@link Order} objects
     */
    @GetMapping
    public ResponseEntity<?> getOrders(Authentication auth) {
        return ResponseEntity.ok(orderService.getUserOrders(auth.getName()));
    }

    /**
     * Verifies a Razorpay payment using the signature returned by the frontend
     * after the Razorpay checkout completes.
     *
     * <p>The HMAC-SHA256 signature is verified server-side using the Razorpay secret
     * key. This ensures the payment details were not tampered with between Razorpay
     * and the frontend. On success, the order status is updated to {@code PAID} and
     * the {@code PaymentTransaction} is updated with the Razorpay Payment ID.</p>
     *
     * <h3>Request</h3>
     * <pre>{@code
     * POST /api/orders/verify
     * Authorization: Bearer <jwt>
     * Content-Type: application/json
     *
     * {
     *   "razorpay_order_id":   "order_XXXXXXXXXX",
     *   "razorpay_payment_id": "pay_XXXXXXXXXX",
     *   "razorpay_signature":  "<hmac-sha256-hex>"
     * }
     * }</pre>
     *
     * <h3>Response (200 OK)</h3>
     * <pre>{@code
     * "Payment verified successfully"
     * }</pre>
     *
     * <h3>Response (422 Unprocessable Entity)</h3>
     * <pre>{@code
     * {
     *   "status": 422,
     *   "error":  "Unprocessable Entity",
     *   "message": "Invalid payment signature"
     * }
     * }</pre>
     *
     * @param request the verification body with Razorpay identifiers and signature
     * @return 200 OK with a success message; 422 if the signature is invalid
     * @throws Exception if the Razorpay SDK throws during signature verification
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestBody PaymentVerificationRequest request) throws Exception {

        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }
}
