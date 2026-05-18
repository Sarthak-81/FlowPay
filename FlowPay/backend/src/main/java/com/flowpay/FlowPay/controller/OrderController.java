package com.flowpay.FlowPay.controller;

import com.flowpay.FlowPay.dto.OrderRequest;
import com.flowpay.FlowPay.dto.OrderResponse;
import com.flowpay.FlowPay.dto.PaymentVerificationRequest;
import com.flowpay.FlowPay.entity.Order;
import com.flowpay.FlowPay.mapper.OrderMapper;
import com.flowpay.FlowPay.service.OrderService;
import com.flowpay.FlowPay.service.PaymentService;
import com.flowpay.FlowPay.utility.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
 * <p>Responses are mapped through {@link OrderMapper} to {@link OrderResponse} DTOs,
 * which eliminates the {@code Order ↔ OrderItem} circular reference that previously
 * caused infinite JSON recursion.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final JwtUtil jwtUtil;
    private final OrderMapper orderMapper;

    /**
     * Creates a new order for the currently authenticated user.
     *
     * @param idempotencyKey optional client-generated key to prevent duplicate orders
     * @param request        the order body containing the list of items to purchase
     * @param auth           Spring Security authentication; {@code auth.getName()} is the user's email
     * @return 201 Created with the persisted order as an {@link OrderResponse} DTO
     * @throws Exception if the Razorpay API call fails
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody OrderRequest request,
            Authentication auth) throws Exception {

        Order order = orderService.createOrder(auth.getName(), request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toOrderResponse(order));
    }

    /**
     * Retrieves all orders belonging to the currently authenticated user.
     *
     * @param auth Spring Security authentication; {@code auth.getName()} is the user's email
     * @return 200 OK with a (possibly empty) list of {@link OrderResponse} DTOs
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(Authentication auth) {
        List<Order> orders = orderService.getUserOrders(auth.getName());
        return ResponseEntity.ok(orderMapper.toOrderResponseList(orders));
    }

    /**
     * Verifies a Razorpay payment using the signature returned by the frontend.
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
