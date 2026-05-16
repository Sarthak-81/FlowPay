package com.flowpay.FlowPay.service;

import com.flowpay.FlowPay.entity.NotificationLog;
import com.flowpay.FlowPay.enums.NotificationStatus;
import com.flowpay.FlowPay.event.PaymentSuccessEvent;
import com.flowpay.FlowPay.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    public void handlePaymentSuccess(PaymentSuccessEvent event) {

        log.info("Preparing notification for successful payment. Order ID: {}", event.getOrderId());

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

        // For Day 3, we simulate notification sending.
        savedLog.setStatus(NotificationStatus.SENT);
        savedLog.setUpdatedAt(LocalDateTime.now());

        notificationLogRepository.save(savedLog);

        log.info("Notification marked as SENT for order {}", event.getOrderId());
    }
}