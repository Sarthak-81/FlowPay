package com.flowpay.FlowPay.service;

import com.flowpay.FlowPay.entity.NotificationLog;
import com.flowpay.FlowPay.enums.NotificationStatus;
import com.flowpay.FlowPay.event.PaymentSuccessEvent;
import com.flowpay.FlowPay.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service responsible for dispatching notifications after a successful payment.
 *
 * <p>This service is invoked by {@link com.flowpay.FlowPay.consumer.PaymentSuccessConsumer}
 * after it consumes a {@code payment.success} Kafka event. The notification lifecycle is:</p>
 * <ol>
 *   <li>Persist a {@link NotificationLog} entry with status {@code PENDING}.</li>
 *   <li>Attempt to send the notification (email, push, SMS — currently simulated).</li>
 *   <li>Update the log entry to {@code SENT} (or {@code FAILED} on error).</li>
 * </ol>
 *
 * <h3>Current state vs. production</h3>
 * <p>Notification sending is currently <b>simulated</b> (the status is set to
 * {@code SENT} immediately without any real delivery). In production, replace
 * the simulation block with an actual provider call (e.g. AWS SES, Twilio, FCM)
 * and handle {@code FAILED} status on delivery errors.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    /**
     * Handles a successful payment event by creating and sending a notification.
     *
     * <p>Creates a {@link NotificationLog} record, simulates notification delivery,
     * and immediately marks it as {@link NotificationStatus#SENT}. All persistence
     * is done in two save calls to maintain an audit trail of the status transition.</p>
     *
     * <p><b>Production TODO:</b> Replace the simulation block with a real notification
     * provider and set status to {@link NotificationStatus#FAILED} on delivery errors,
     * triggering a retry mechanism.</p>
     *
     * @param event the payment success event consumed from the Kafka {@code payment.success} topic;
     *              contains order ID, Razorpay IDs, and the payment amount
     */
    public void handlePaymentSuccess(PaymentSuccessEvent event) {

        log.info("Preparing notification for successful payment. Order ID: {}", event.getOrderId());

        // Create the initial log entry in PENDING state before attempting delivery.
        // This ensures the event is recorded even if the send step fails.
        NotificationLog notificationLog = NotificationLog.builder()
                .orderId(event.getOrderId())
                .razorpayOrderId(event.getRazorpayOrderId())
                .razorpayPaymentId(event.getRazorpayPaymentId())
                .amount(event.getAmount())
                .eventType("payment.success")
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        NotificationLog savedLog = notificationLogRepository.save(notificationLog);

        log.info("Notification log created for order {}", event.getOrderId());

        // TODO (Day 3): Replace this simulation with a real provider call.
        // e.g. emailClient.sendPaymentConfirmation(event.getUserEmail(), event.getAmount());
        // On success: set SENT. On exception: set FAILED and schedule a retry.
        savedLog.setStatus(NotificationStatus.SENT);
        savedLog.setUpdatedAt(LocalDateTime.now());

        notificationLogRepository.save(savedLog);

        log.info("Notification marked as SENT for order {}", event.getOrderId());
    }
}
