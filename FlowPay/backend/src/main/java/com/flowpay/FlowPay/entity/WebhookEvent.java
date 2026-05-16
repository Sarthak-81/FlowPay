package com.flowpay.FlowPay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;

    private String eventType;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private String signature;

    private Boolean processed;

    private LocalDateTime receivedAt;

    private LocalDateTime processedAt;
}