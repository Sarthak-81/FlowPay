package com.flowpay.FlowPay.service;

import com.flowpay.FlowPay.entity.PaymentEvent;
import com.flowpay.FlowPay.entity.PaymentTransaction;
import com.flowpay.FlowPay.repository.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentEventService {

    private final PaymentEventRepository paymentEventRepository;

    public void recordEvent(
            PaymentTransaction transaction,
            String eventType,
            String oldStatus,
            String newStatus,
            String message
    ) {
        PaymentEvent event = PaymentEvent.builder()
                .paymentTransactionId(transaction.getId())
                .razorpayOrderId(transaction.getRazorpayOrderId())
                .eventType(eventType)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        paymentEventRepository.save(event);
    }
}