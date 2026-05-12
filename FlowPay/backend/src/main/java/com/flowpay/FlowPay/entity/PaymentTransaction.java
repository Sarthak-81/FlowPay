package com.flowpay.FlowPay.entity;

import java.time.LocalDateTime;

import com.flowpay.FlowPay.enums.PaymentStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction 
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String webhookEvent;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}