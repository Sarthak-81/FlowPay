package com.flowpay.FlowPay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.flowpay.FlowPay.dto.RazorpayWebhookRequest;
import com.flowpay.FlowPay.service.PaymentService;

public class PaymentController 
{
    @Autowired
    private PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody RazorpayWebhookRequest request,
            @RequestHeader("X-Razorpay-Signature") String signature) 
    {
        paymentService.processWebhook(request, signature);
        return ResponseEntity.ok("Webhook processed");
    }    
}
