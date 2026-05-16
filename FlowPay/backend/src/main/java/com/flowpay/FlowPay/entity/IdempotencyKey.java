package com.flowpay.FlowPay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    private String userEmail;

    private String requestHash;

    private String resourceType;

    private String resourceId;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
}