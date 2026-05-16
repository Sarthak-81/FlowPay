package com.flowpay.FlowPay.service;

import com.flowpay.FlowPay.dto.PaymentVerificationRequest;
import com.flowpay.FlowPay.dto.RazorpayWebhookRequest;
import com.flowpay.FlowPay.entity.Order;
import com.flowpay.FlowPay.entity.PaymentTransaction;
import com.flowpay.FlowPay.entity.WebhookEvent;
import com.flowpay.FlowPay.enums.PaymentStatus;
import com.flowpay.FlowPay.event.PaymentSuccessEvent;
import com.flowpay.FlowPay.repository.OrderRepository;
import com.flowpay.FlowPay.repository.PaymentTransactionRepository;
import com.flowpay.FlowPay.repository.WebhookEventRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentEventService paymentEventService;
    private final WebhookEventRepository webhookEventRepository;

    public String createRazorpayOrder(Double amount) throws Exception {

        RazorpayClient client = new RazorpayClient(key, secret);

        JSONObject options = new JSONObject();
        options.put("amount", (long) (amount * 100));
        options.put("currency", "INR");
        options.put("receipt", "txn_" + System.currentTimeMillis());

        com.razorpay.Order order = client.orders.create(options);

        return order.get("id").toString();
    }

    public String verifyPayment(PaymentVerificationRequest request) throws Exception {

        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();

        boolean isValid = Utils.verifySignature(
                payload,
                request.getRazorpaySignature(),
                secret
        );

        if (!isValid) {
            throw new RuntimeException("Invalid payment signature");
        }

        Order order = orderRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() ->
                        new RuntimeException("Order not found: " + request.getRazorpayOrderId()));

        order.setStatus("PAID");
        orderRepository.save(order);

        PaymentTransaction transaction = paymentTransactionRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() ->
                        new RuntimeException("Transaction not found: " + request.getRazorpayOrderId()));

        PaymentStatus oldStatus = transaction.getStatus();

        transaction.setRazorpayPaymentId(request.getRazorpayPaymentId());
        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setUpdatedAt(LocalDateTime.now());

        PaymentTransaction savedTransaction =
                paymentTransactionRepository.save(transaction);

        paymentEventService.recordEvent(
                savedTransaction,
                "PAYMENT_VERIFIED",
                oldStatus != null ? oldStatus.name() : null,
                PaymentStatus.SUCCESS.name(),
                "Payment verified using Razorpay signature"
        );

        log.info(
                "Payment verified. Order {} marked PAID. Transaction updated with payment ID {}",
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId()
        );

        return "Payment verified successfully";
    }

    public void processWebhook(RazorpayWebhookRequest request, String signature) {

        log.info("Webhook received: {}", request.getEvent());

        String paymentId = request.getPayload()
                .getPayment()
                .getEntity()
                .getId();

        String orderId = request.getPayload()
                .getPayment()
                .getEntity()
                .getOrder_id();

        WebhookEvent webhookEvent = WebhookEvent.builder()
                .source("RAZORPAY")
                .eventType(request.getEvent())
                .razorpayOrderId(orderId)
                .razorpayPaymentId(paymentId)
                .signature(signature)
                .processed(false)
                .receivedAt(LocalDateTime.now())
                .build();

        WebhookEvent savedWebhook = webhookEventRepository.save(webhookEvent);

        PaymentTransaction transaction = paymentTransactionRepository
                .findByRazorpayOrderId(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Transaction not found: " + orderId));

        PaymentStatus oldStatus = transaction.getStatus();

        transaction.setRazorpayPaymentId(paymentId);
        transaction.setWebhookEvent(request.getEvent());
        transaction.setUpdatedAt(LocalDateTime.now());

        if ("payment.captured".equals(request.getEvent())) {

            transaction.setStatus(PaymentStatus.SUCCESS);

            PaymentTransaction savedTransaction =
                    paymentTransactionRepository.save(transaction);

            paymentEventService.recordEvent(
                    savedTransaction,
                    "PAYMENT_SUCCESS",
                    oldStatus != null ? oldStatus.name() : null,
                    PaymentStatus.SUCCESS.name(),
                    "Payment captured via Razorpay webhook"
            );

            PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                    .orderId(savedTransaction.getOrderId())
                    .razorpayOrderId(savedTransaction.getRazorpayOrderId())
                    .razorpayPaymentId(savedTransaction.getRazorpayPaymentId())
                    .amount(savedTransaction.getAmount())
                    .status(savedTransaction.getStatus().name())
                    .eventTime(LocalDateTime.now())
                    .build();

            paymentEventProducer.publishPaymentSuccess(event);

            savedWebhook.setProcessed(true);
            savedWebhook.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(savedWebhook);

            log.info("Payment captured successfully for order {}", orderId);

        } else {

            transaction.setStatus(PaymentStatus.FAILED);

            PaymentTransaction savedTransaction =
                    paymentTransactionRepository.save(transaction);

            paymentEventService.recordEvent(
                    savedTransaction,
                    "PAYMENT_FAILED",
                    oldStatus != null ? oldStatus.name() : null,
                    PaymentStatus.FAILED.name(),
                    "Payment failed via Razorpay webhook"
            );

            savedWebhook.setProcessed(true);
            savedWebhook.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(savedWebhook);

            log.error("Payment failed for order {}", orderId);
        }
    }
}