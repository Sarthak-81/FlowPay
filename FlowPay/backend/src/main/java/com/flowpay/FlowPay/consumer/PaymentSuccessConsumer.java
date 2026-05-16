package com.flowpay.FlowPay.consumer;

import com.flowpay.FlowPay.event.PaymentSuccessEvent;
import com.flowpay.FlowPay.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for successful payment events.
 *
 * <p>Subscribes to the {@code payment.success} Kafka topic and delegates each
 * consumed event to {@link NotificationService#handlePaymentSuccess} to trigger
 * the notification flow (e.g. sending a payment confirmation email or push notification).</p>
 *
 * <h3>Kafka configuration</h3>
 * <ul>
 *   <li><b>Topic:</b> {@code payment.success} — must match the topic used by
 *       {@link com.flowpay.FlowPay.service.PaymentEventProducer}.</li>
 *   <li><b>Consumer group:</b> {@code flowpay-notification-group} — all instances
 *       of this service share this group, so each event is processed by exactly one
 *       instance (horizontal scaling).</li>
 * </ul>
 *
 * <h3>Error handling</h3>
 * <p>If {@link NotificationService#handlePaymentSuccess} throws an exception, the
 * Kafka consumer will retry based on the configured retry policy. Consider adding a
 * Dead Letter Topic (DLT) to capture events that cannot be processed after all retries.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSuccessConsumer {

    private final NotificationService notificationService;

    /**
     * Consumes a {@link PaymentSuccessEvent} from the {@code payment.success} Kafka topic.
     *
     * <p>This method is invoked by the Kafka listener container framework once per
     * message. The event is deserialized automatically using the configured
     * {@code JsonDeserializer} before this method is called.</p>
     *
     * <p>Delegates to {@link NotificationService#handlePaymentSuccess} for the actual
     * notification logic. Any exception thrown will propagate to the Kafka listener
     * container, which will handle retries according to the application's retry policy.</p>
     *
     * @param event the deserialized payment success event containing order and payment details
     */
    @KafkaListener(
            topics = "payment.success",
            groupId = "flowpay-notification-group"
    )
    public void consumePaymentSuccess(PaymentSuccessEvent event) {

        log.info("Consumed payment.success event for order {}", event.getOrderId());

        notificationService.handlePaymentSuccess(event);
    }
}
