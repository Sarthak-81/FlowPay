package com.flowpay.FlowPay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flowpay.FlowPay.dto.OrderRequest;
import com.flowpay.FlowPay.dto.PaymentVerificationRequest;
import com.flowpay.FlowPay.service.OrderService;
import com.flowpay.FlowPay.service.PaymentService;

/**
 * REST controller for order management and payment verification.
 *
 * <p>Exposes three endpoints under {@code /api/orders}:</p>
 * <ul>
 *   <li>{@code POST /api/orders}        – Creates a new order and a Razorpay order</li>
 *   <li>{@code GET  /api/orders}        – Retrieves all orders for the authenticated user</li>
 *   <li>{@code POST /api/orders/verify} – Verifies a Razorpay payment signature</li>
 * </ul>
 *
 * <p>All endpoints require a valid JWT Bearer token (enforced by {@link com.flowpay.FlowPay.config.JwtFilter}).</p>
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    /**
     * Creates a new order for the currently authenticated user.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Persists an {@code Order} entity with status {@code CREATED}.</li>
     *   <li>Calls the Razorpay API to create a corresponding Razorpay order.</li>
     *   <li>Persists a {@code PaymentTransaction} record linked to the order.</li>
     *   <li>Returns the saved {@code Order} (which includes the {@code razorpayOrderId}
     *       the frontend needs to open the Razorpay checkout).</li>
     * </ol>
     *
     * @param request the order request body containing the amount
     * @param auth    Spring Security authentication object; {@code auth.getName()} is the user's email
     * @return the created {@link com.flowpay.FlowPay.entity.Order}
     * @throws Exception if the Razorpay API call fails
     */
    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody OrderRequest request,
            Authentication auth) throws Exception {

        return ResponseEntity.ok(orderService.createOrder(auth.getName(), request));
    }

    /**
     * Retrieves all orders belonging to the currently authenticated user.
     *
     * @param auth Spring Security authentication object; {@code auth.getName()} is the user's email
     * @return a list of {@link com.flowpay.FlowPay.entity.Order} objects
     */
    @GetMapping
    public ResponseEntity<?> getOrders(Authentication auth) {
        return ResponseEntity.ok(orderService.getUserOrders(auth.getName()));
    }

    /**
     * Verifies a Razorpay payment using the signature returned by the frontend
     * after the Razorpay checkout completes.
     *
     * <p>The request body must contain {@code razorpay_order_id},
     * {@code razorpay_payment_id}, and {@code razorpay_signature} exactly as
     * returned by the Razorpay JavaScript SDK (snake_case). Jackson maps these
     * to the corresponding fields in {@link PaymentVerificationRequest} via
     * {@code @JsonProperty} annotations.</p>
     *
     * @param request the verification request with Razorpay identifiers and signature
     * @return a success message if the signature is valid; throws an exception otherwise
     * @throws Exception if signature verification fails or the order is not found
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestBody PaymentVerificationRequest request) throws Exception {

        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }
}
