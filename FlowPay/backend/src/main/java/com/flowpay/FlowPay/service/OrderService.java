package com.flowpay.FlowPay.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.flowpay.FlowPay.dto.OrderRequest;
import com.flowpay.FlowPay.entity.Order;
import com.flowpay.FlowPay.entity.PaymentTransaction;
import com.flowpay.FlowPay.enums.PaymentStatus;
import com.flowpay.FlowPay.repository.OrderRepository;
import com.flowpay.FlowPay.repository.PaymentTransactionRepository;

/**
 * Service layer for order lifecycle management.
 *
 * <p>Handles order creation (including Razorpay order generation and
 * initial {@code PaymentTransaction} logging), order retrieval, and
 * in-process payment verification as a fallback to webhook-based updates.</p>
 */
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private PaymentService paymentService;

    @Value("${razorpay.key}")
    private String key;

    @Value("${razorpay.secret}")
    private String secret;

    /**
     * Creates a new order for the given user.
     *
     * <p>Steps performed:</p>
     * <ol>
     *   <li>Persists an {@link Order} with status {@code CREATED}.</li>
     *   <li>Calls {@link PaymentService#createRazorpayOrder} to obtain a
     *       Razorpay Order ID from the Razorpay API.</li>
     *   <li>Stores the Razorpay Order ID on the saved order and re-persists.</li>
     *   <li>Creates a {@link PaymentTransaction} audit record linked to the order.</li>
     * </ol>
     *
     * <p>The returned {@link Order} contains the {@code razorpayOrderId} that
     * the frontend must pass to the Razorpay checkout SDK.</p>
     *
     * @param email   the email address of the authenticated user (extracted from JWT)
     * @param request the incoming order request containing the payment amount
     * @return the persisted {@link Order} entity
     * @throws Exception if the Razorpay API call fails
     */
    public Order createOrder(String email, OrderRequest request) throws Exception {

        // Build and persist the initial order record
        Order order = new Order();
        order.setAmount(request.getAmount());
        order.setStatus("CREATED");
        order.setUserEmail(email);
        order.setCreatedAt(LocalDateTime.now());

        // Call Razorpay to generate an order ID
        String razorpayOrderId = paymentService.createRazorpayOrder(request.getAmount());
        order.setRazorpayOrderId(razorpayOrderId);

        Order savedOrder = orderRepository.save(order);

        // Persist an initial PaymentTransaction for audit trail
        PaymentTransaction transaction = PaymentTransaction.builder()
                .orderId(savedOrder.getId().toString())
                .razorpayOrderId(savedOrder.getRazorpayOrderId())
                .amount(savedOrder.getAmount())
                .status(PaymentStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paymentTransactionRepository.save(transaction);

        return savedOrder;
    }

    /**
     * Retrieves all orders associated with the given user email.
     *
     * @param email the email address of the authenticated user
     * @return a list of {@link Order} entities; empty list if none found
     */
    public List<Order> getUserOrders(String email) {
        return orderRepository.findByUserEmail(email);
    }
}
