package com.flowpay.FlowPay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication; 
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flowpay.FlowPay.dto.OrderRequest;
import com.flowpay.FlowPay.dto.PaymentVerificationRequest;
import com.flowpay.FlowPay.service.OrderService;
import com.flowpay.FlowPay.service.PaymentService;

@RestController
@RequestMapping("/api/orders")
public class OrderController 
{
    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request, Authentication auth) throws Exception 
    {
        return ResponseEntity.ok(orderService.createOrder(auth.getName(), request));
    }

    @GetMapping
    public ResponseEntity<?> getOrders(Authentication auth) 
    {
        return ResponseEntity.ok(orderService.getUserOrders(auth.getName()));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerificationRequest request) throws Exception 
    {
        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }
}
