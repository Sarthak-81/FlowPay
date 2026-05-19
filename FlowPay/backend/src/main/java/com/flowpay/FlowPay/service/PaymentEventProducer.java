package com.flowpay.FlowPay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowpay.FlowPay.entity.FailedEvent;
import com.flowpay.FlowPay.event.PaymentSuccessEvent;
import com.flowpay.FlowPay.repository.FailedEventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentSuccessEvent> kafkaTemplate;
    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;

    private static final String PAYMENT_SUCCESS_TOPIC = "payment.success";

    @CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "fallbackPaymentSuccess")
    public void publishPaymentSuccess(PaymentSuccessEvent event) {

        try {
            log.info("Publishing payment success event for order {}", event.getOrderId());

            kafkaTemplate
                    .send(PAYMENT_SUCCESS_TOPIC, event.getOrderId(), event)
                    .get();

            log.info("Payment success event published for order {}", event.getOrderId());

        } catch (Exception ex) {
            throw new RuntimeException("Kafka publish failed", ex);
        }
    }

    public void fallbackPaymentSuccess(PaymentSuccessEvent event, Throwable ex) {

        log.error("Kafka publish failed. Saving event to failed_events. Order {}",
                event.getOrderId());

        try {
            FailedEvent failedEvent = FailedEvent.builder()
                    .topic(PAYMENT_SUCCESS_TOPIC)
                    .eventType("payment.success")
                    .aggregateId(event.getOrderId())
                    .payload(objectMapper.writeValueAsString(event))
                    .retryCount(0)
                    .status("PENDING")
                    .errorMessage(ex.getMessage())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            failedEventRepository.save(failedEvent);

            log.info("Failed event saved for retry. Order {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Could not save failed Kafka event", e);
        }
    }
}