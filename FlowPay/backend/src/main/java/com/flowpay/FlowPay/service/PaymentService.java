package com.flowpay.FlowPay.service;

import java.time.LocalDateTime;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.flowpay.FlowPay.dto.PaymentVerificationRequest;
import com.flowpay.FlowPay.dto.RazorpayWebhookRequest;
import com.flowpay.FlowPay.entity.Order;
import com.flowpay.FlowPay.entity.PaymentTransaction;
import com.flowpay.FlowPay.enums.PaymentStatus;
import com.flowpay.FlowPay.repository.OrderRepository;
import com.flowpay.FlowPay.repository.PaymentTransactionRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    @Value("${razorpay.key}")
    private String key;

    @Value("${razorpay.secret}")
    private String secret;

    private final OrderRepository orderRepository;

    private final PaymentTransactionRepository paymentTransactionRepository;

    public String createRazorpayOrder(Double amount) throws Exception {

        RazorpayClient client = new RazorpayClient(key, secret);

        JSONObject options = new JSONObject();
        options.put("amount", amount * 100); // paise
        options.put("currency", "INR");
        options.put("receipt", "txn_123456");

        com.razorpay.Order order = client.orders.create(options);

        return order.get("id").toString();
    }

     public String verifyPayment(PaymentVerificationRequest request) throws Exception {

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

    public void processWebhook(RazorpayWebhookRequest request, String signature) 
    {
        log.info("Received Razorpay webhook: {}", request.getEvent());

        String paymentId = request.getPayload()
                .getPayment()
                .getEntity()
                .getId();

        String orderId = request.getPayload()
                .getPayment()
                .getEntity()
                .getOrder_id();

        PaymentTransaction transaction = paymentTransactionRepository
                .findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setRazorpayPaymentId(paymentId);
        transaction.setWebhookEvent(request.getEvent());
        transaction.setUpdatedAt(LocalDateTime.now());

        if ("payment.captured".equals(request.getEvent())) {
            transaction.setStatus(PaymentStatus.SUCCESS);
            log.info("Payment captured successfully for order {}", orderId);
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            log.error("Payment failed for order {}", orderId);
        }

        paymentTransactionRepository.save(transaction);
    }
}