package com.flowpay.FlowPay.service;

import com.flowpay.FlowPay.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProducer 
{

    private final KafkaTemplate<String, PaymentSuccessEvent> kafkaTemplate;

    private static final String PAYMENT_SUCCESS_TOPIC = "payment.success";

    public void publishPaymentSuccess(PaymentSuccessEvent event) {

        log.info("Publishing payment success event for order {}", event.getOrderId());

        kafkaTemplate.send(
                PAYMENT_SUCCESS_TOPIC,
                event.getOrderId(),
                event
        );

        log.info("Payment success event published for order {}", event.getOrderId());
    }
}