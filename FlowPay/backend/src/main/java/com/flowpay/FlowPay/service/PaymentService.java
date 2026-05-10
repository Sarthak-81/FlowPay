package com.flowpay.FlowPay.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.flowpay.FlowPay.dto.PaymentVerificationRequest;
import com.flowpay.FlowPay.entity.Order;
import com.flowpay.FlowPay.repository.OrderRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;

@Service
public class PaymentService {

    @Value("${razorpay.key}")
    private String key;

    @Value("${razorpay.secret}")
    private String secret;

    @Autowired
    private OrderRepository orderRepository;

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
}