package com.flowpay.FlowPay.service;

import com.flowpay.FlowPay.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka producer for payment-related events.
 *
 * <p>Publishes domain events to Kafka topics so that downstream consumers
 * (e.g. {@link com.flowpay.FlowPay.consumer.PaymentSuccessConsumer}) can react
 * asynchronously without being coupled to the payment processing flow.</p>
 *
 * <p>The Kafka topic name is kept as a constant to avoid magic strings scattered
 * across the codebase. Both producer and consumer must reference the same topic.</p>
 *
 * <h3>Message format</h3>
 * <p>Messages are keyed by {@code orderId} (a string), which ensures that all
 * events for the same order are routed to the same Kafka partition, preserving
 * ordering guarantees for that order's event stream.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentSuccessEvent> kafkaTemplate;

    /**
     * Kafka topic to which successful payment events are published.
     * Must match the topic used by {@link com.flowpay.FlowPay.consumer.PaymentSuccessConsumer}.
     */
    private static final String PAYMENT_SUCCESS_TOPIC = "payment.success";

    /**
     * Publishes a {@link PaymentSuccessEvent} to the {@code payment.success} Kafka topic.
     *
     * <p>The message key is {@code event.getOrderId()}, ensuring all events for
     * the same order land in the same partition and are consumed in order.</p>
     *
     * <p>This is a fire-and-forget call ({@code kafkaTemplate.send} is non-blocking).
     * Add a {@code ListenableFuture} callback if delivery confirmation is required.</p>
     *
     * @param event the payment success event containing order, payment, and amount details;
     *              must not be {@code null}
     */
    public void publishPaymentSuccess(PaymentSuccessEvent event) {

        log.info("Publishing payment success event for order {}", event.getOrderId());

        kafkaTemplate.send(
                PAYMENT_SUCCESS_TOPIC,
                event.getOrderId(), // message key — routes same-order events to the same partition
                event
        );

        log.info("Payment success event published for order {}", event.getOrderId());
    }
}
