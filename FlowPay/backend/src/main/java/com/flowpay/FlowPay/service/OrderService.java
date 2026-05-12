package com.flowpay.FlowPay.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.flowpay.FlowPay.dto.OrderRequest;
import com.flowpay.FlowPay.dto.PaymentVerificationRequest;
import com.flowpay.FlowPay.entity.Order;
import com.flowpay.FlowPay.entity.PaymentTransaction;
import com.flowpay.FlowPay.enums.PaymentStatus;
import com.flowpay.FlowPay.repository.OrderRepository;
import com.flowpay.FlowPay.repository.PaymentTransactionRepository;
//import com.razorpay.Utils;
import com.razorpay.Utils;

@Service
public class OrderService 
{
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
    
    public Order createOrder(String email, OrderRequest request) throws Exception 
    {

        Order order = new Order();
        order.setAmount(request.amount);
        order.setStatus("CREATED");
        order.setUserEmail(email);
        order.setCreatedAt(LocalDateTime.now());

        String razorpayOrderId = paymentService.createRazorpayOrder(request.getAmount());

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

        paymentTransactionRepository.save(transaction);

        return savedOrder;
    }

    public List<Order> getUserOrders(String email) 
    {
        return orderRepository.findByUserEmail(email);
    }

    public String verifyPayment(PaymentVerificationRequest request) throws Exception 
    {
        String payload = request.razorpayOrderId + "|" + request.razorpayPaymentId;

        boolean isValid = Utils.verifySignature(
                payload,
                request.razorpaySignature,
                secret
        );

        if (!isValid) {
            throw new RuntimeException("Invalid payment signature");
        }

        Order order = orderRepository
                .findByRazorpayOrderId(request.razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus("PAID");

        orderRepository.save(order);

        return "Payment verified successfully";
    }
}
