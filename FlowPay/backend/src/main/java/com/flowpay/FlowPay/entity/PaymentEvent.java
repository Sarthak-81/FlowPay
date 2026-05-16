package com.flowpay.FlowPay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long paymentTransactionId;

    private String razorpayOrderId;

    private String eventType;

    private String oldStatus;

    private String newStatus;

    private String message;

    private LocalDateTime createdAt;
}