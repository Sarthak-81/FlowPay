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
     * <h3>Idempotency flow</h3>
     * <p>If {@code idempotencyKey} is non-null and non-blank:</p>
     * <ol>
     *   <li>Look up the key in {@code idempotency_keys}.</li>
     *   <li>If found and the resource type is {@code ORDER}, return the original order.</li>
     *   <li>If not found, proceed with order creation and persist the key at the end.</li>
     * </ol>
     * <p>This prevents double-charges when clients retry a request after a timeout.</p>
     *
     * <h3>Order creation steps</h3>
     * <ol>
     *   <li>Validate the items list (must be non-empty).</li>
     *   <li>Compute {@code totalAmount} from item quantities and unit prices.</li>
     *   <li>Build and save the {@link Order} entity (without Razorpay ID yet).</li>
     *   <li>Call {@link PaymentService#createRazorpayOrder} to get the Razorpay Order ID.</li>
     *   <li>Set the Razorpay Order ID on the order and re-save.</li>
     *   <li>Persist an initial {@link PaymentTransaction} with status {@code CREATED}.</li>
     *   <li>Record a {@code PAYMENT_CREATED} audit event.</li>
     *   <li>Persist the idempotency key if one was supplied.</li>
     * </ol>
     *
     * @param email          the authenticated user's email (extracted from JWT by the controller)
     * @param request        the order request body with the list of items
     * @param idempotencyKey optional client-supplied key for duplicate-request protection;
     *                       pass {@code null} or blank to skip idempotency checks
     * @return the persisted {@link Order} entity, including its Razorpay Order ID
     * @throws IllegalArgumentException  if the items list is null or empty
     * @throws ResourceNotFoundException if an idempotency key references a missing order
     * @throws Exception                 if the Razorpay API call fails
     */
    public Order createOrder(String email, OrderRequest request, String idempotencyKey) throws Exception {

        // Guard: items must be present and non-empty to compute a meaningful total.
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }

        // ── Idempotency check ────────────────────────────────────────────────
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {

            Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);

            if (existing.isPresent() && "ORDER".equals(existing.get().getResourceType())) {
                // A request with this key was already processed — return the original order.
                return orderRepository.findById(Long.valueOf(existing.get().getResourceId()))
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Idempotency key references a missing order (ID: "
                                        + existing.get().getResourceId() + "). Data may be inconsistent."));
            }
        }

        // ── Build the Order entity ───────────────────────────────────────────
        Order order = new Order();
        order.setStatus("CREATED");
        order.setUserEmail(email);
        order.setCreatedAt(LocalDateTime.now());

        // Compute totalAmount before setting items so the cascade save includes it.
        double totalAmount = request.getItems().stream()
                .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                .sum();

        order.setAmount(totalAmount);

        // Map DTOs → entities; each OrderItem holds a back-reference to its parent Order
        // so the JPA cascade can save them in the same transaction.
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
        // This call is made BEFORE the first DB save so that if Razorpay is down,
        // we avoid persisting an Order with no Razorpay ID.
        String razorpayOrderId = paymentService.createRazorpayOrder(totalAmount);
        order.setRazorpayOrderId(razorpayOrderId);

        Order savedOrder = orderRepository.save(order);

        // ── Record the initial PaymentTransaction ───────────────────────────
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
     * <p>Returns an empty list (not an exception) if the user has no orders,
     * which is a valid state for a newly registered user.</p>
     *
     * @param email the authenticated user's email
     * @return a (possibly empty) list of {@link Order} entities sorted by creation time
     */
    public List<Order> getUserOrders(String email) {
        return orderRepository.findByUserEmail(email);
    }
}
