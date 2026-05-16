package com.flowpay.FlowPay.consumer;

import com.flowpay.FlowPay.event.PaymentSuccessEvent;
import com.flowpay.FlowPay.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSuccessConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "payment.success",
            groupId = "flowpay-notification-group"
    )
    public void consumePaymentSuccess(PaymentSuccessEvent event) {

        log.info("Consumed payment.success event for order {}", event.getOrderId());

        notificationService.handlePaymentSuccess(event);
    }
}