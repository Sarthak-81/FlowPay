package com.flowpay.FlowPay.service;

import com.flowpay.FlowPay.dto.OrderRequest;
import com.flowpay.FlowPay.entity.IdempotencyKey;
import com.flowpay.FlowPay.entity.Order;
import com.flowpay.FlowPay.entity.OrderItem;
import com.flowpay.FlowPay.entity.PaymentTransaction;
import com.flowpay.FlowPay.enums.PaymentStatus;
import com.flowpay.FlowPay.exception.ResourceNotFoundException;
import com.flowpay.FlowPay.repository.IdempotencyKeyRepository;
import com.flowpay.FlowPay.repository.OrderRepository;
import com.flowpay.FlowPay.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for order lifecycle management.
 *
 * <p>Handles:</p>
 * <ul>
 *   <li><b>Order creation</b>: Persists an {@link Order}, calls the Razorpay Orders API,
 *       and records an initial {@link PaymentTransaction} audit entry.</li>
 *   <li><b>Idempotency</b>: If a client-supplied {@code Idempotency-Key} matches an
 *       already-processed request, the original order is returned without creating a
 *       duplicate charge.</li>
 *   <li><b>Order retrieval</b>: Fetches all orders for a given user email.</li>
 * </ul>
 *
 * <p>Custom exceptions thrown here bubble up to
 * {@link com.flowpay.FlowPay.exception.GlobalExceptionHandler} for HTTP mapping.</p>
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentEventService paymentEventService;
    private final PaymentService paymentService;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    /**
     * Creates a new order for the given user, with optional idempotency support.
     *
     * @param email          the authenticated user's email (extracted from JWT by the controller)
     * @param request        the order request body with the list of items
     * @param idempotencyKey optional client-supplied key for duplicate-request protection
     * @return the persisted {@link Order} entity, including its Razorpay Order ID
     * @throws IllegalArgumentException  if the items list is null or empty
     * @throws ResourceNotFoundException if an idempotency key references a missing order
     * @throws Exception                 if the Razorpay API call fails
     */
    public Order createOrder(String email, OrderRequest request, String idempotencyKey) throws Exception {

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }

        // ── Idempotency check ────────────────────────────────────────────────
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent() && "ORDER".equals(existing.get().getResourceType())) {
                return orderRepository.findById(Long.valueOf(existing.get().getResourceId()))
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Idempotency key references a missing order (ID: "
                                        + existing.get().getResourceId() + "). Data may be inconsistent."));
            }
        }

        double totalAmount = request.getItems().stream()
                .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                .sum();

        Order order = Order.builder()
                .status("CREATED")
                .userEmail(email)
                .createdAt(LocalDateTime.now())
                .amount(totalAmount)
                .build();

        // Map DTOs → entities; each OrderItem holds a back-reference to its parent Order.
        List<OrderItem> orderItems = request.getItems().stream()
                .map(itemRequest -> OrderItem.builder()
                        .itemName(itemRequest.getItemName())
                        .quantity(itemRequest.getQuantity())
                        .unitPrice(itemRequest.getUnitPrice())
                        .totalPrice(itemRequest.getQuantity() * itemRequest.getUnitPrice())
                        .order(order)
                        .build())
                .toList();

        order.setItems(orderItems);

        // ── Create the Razorpay order ────────────────────────────────────────
        String razorpayOrderId = paymentService.createRazorpayOrder(totalAmount);
        order.setRazorpayOrderId(razorpayOrderId);

        Order savedOrder = orderRepository.save(order);

        // ── Record the initial PaymentTransaction ────────────────────────────
        PaymentTransaction transaction = PaymentTransaction.builder()
                .orderId(savedOrder.getId().toString())
                .razorpayOrderId(savedOrder.getRazorpayOrderId())
                .amount(savedOrder.getAmount())
                .status(PaymentStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

        paymentEventService.recordEvent(
                savedTransaction,
                "PAYMENT_CREATED",
                null,
                PaymentStatus.CREATED.name(),
                "Payment transaction created"
        );

        // ── Persist the idempotency key ──────────────────────────────────────
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyKey idem = IdempotencyKey.builder()
                    .idempotencyKey(idempotencyKey)
                    .userEmail(email)
                    .requestHash(String.valueOf(request.hashCode()))
                    .resourceType("ORDER")
                    .resourceId(savedOrder.getId().toString())
                    .status("COMPLETED")
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();

            idempotencyKeyRepository.save(idem);
        }

        return savedOrder;
    }

    /**
     * Retrieves all orders associated with the given user email.
     *
     * @param email the authenticated user's email
     * @return a (possibly empty) list of {@link Order} entities
     */
    public List<Order> getUserOrders(String email) {
        return orderRepository.findByUserEmail(email);
    }
}
