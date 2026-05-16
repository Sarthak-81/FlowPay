package com.flowpay.FlowPay.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.flowpay.FlowPay.dto.OrderRequest;
import com.flowpay.FlowPay.entity.Order;
import com.flowpay.FlowPay.entity.OrderItem;
import com.flowpay.FlowPay.entity.PaymentTransaction;
import com.flowpay.FlowPay.enums.PaymentStatus;
import com.flowpay.FlowPay.repository.OrderRepository;
import com.flowpay.FlowPay.repository.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service layer for order lifecycle management.
 *
 * <p>Handles order creation (including Razorpay order generation and
 * initial {@code PaymentTransaction} logging), order retrieval, and
 * in-process payment verification as a fallback to webhook-based updates.</p>
 */
@Service
@RequiredArgsConstructor
public class OrderService 
{

    private final OrderRepository orderRepository;

    private final PaymentTransactionRepository paymentTransactionRepository;

    private final PaymentEventService paymentEventService;

    private final PaymentService paymentService;

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
    public Order createOrder(String email, OrderRequest request) throws Exception 
    {

        Order order = new Order();
    order.setStatus("CREATED");
    order.setUserEmail(email);
    order.setCreatedAt(LocalDateTime.now());

    double totalAmount = request.getItems()
            .stream()
            .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
            .sum();

    order.setAmount(totalAmount);

    List<OrderItem> orderItems = request.getItems()
            .stream()
            .map(itemRequest -> {
                OrderItem item = OrderItem.builder()
                        .itemName(itemRequest.getItemName())
                        .quantity(itemRequest.getQuantity())
                        .unitPrice(itemRequest.getUnitPrice())
                        .totalPrice(itemRequest.getQuantity() * itemRequest.getUnitPrice())
                        .order(order)
                        .build();

                return item;
            })
            .toList();

    order.setItems(orderItems);

    String razorpayOrderId = paymentService.createRazorpayOrder(totalAmount);

    order.setRazorpayOrderId(razorpayOrderId);

    Order savedOrder = orderRepository.save(order);

    PaymentTransaction transaction = PaymentTransaction.builder()
            .orderId(savedOrder.getId().toString())
            .razorpayOrderId(savedOrder.getRazorpayOrderId())
            .amount(savedOrder.getAmount())
            .status(PaymentStatus.CREATED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    PaymentTransaction savedTransaction =
            paymentTransactionRepository.save(transaction);

    paymentEventService.recordEvent(
            savedTransaction,
            "PAYMENT_CREATED",
            null,
            PaymentStatus.CREATED.name(),
            "Payment transaction created"
    );

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
