package com.flowpay.FlowPay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flowpay.FlowPay.dto.RazorpayWebhookRequest;
import com.flowpay.FlowPay.service.PaymentService;

/**
 * REST controller that handles Razorpay webhook events.
 *
 * <p>Razorpay calls this endpoint after key payment lifecycle events
 * (e.g. {@code payment.captured}, {@code payment.failed}).
 * The incoming request is verified using the {@code X-Razorpay-Signature}
 * header before any state changes are made.</p>
 *
 * <p>Base URL: {@code /api/payments}</p>
 *
 * <p><b>Fix:</b> The {@code @RestController} and {@code @RequestMapping}
 * annotations were missing from this class, which meant Spring never
 * registered it as a bean or mapped any of its endpoints.</p>
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Handles incoming Razorpay webhook POST requests.
     *
     * <p>Delegates processing to {@link PaymentService#processWebhook} which
     * updates the {@code PaymentTransaction} record based on the event type.</p>
     *
     * @param request   the parsed webhook body containing the event and payload
     * @param signature the HMAC-SHA256 signature from the {@code X-Razorpay-Signature} header
     * @return a 200 OK response with a confirmation message
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody RazorpayWebhookRequest request,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        paymentService.processWebhook(request, signature);
        return ResponseEntity.ok("Webhook processed");
    }
}
