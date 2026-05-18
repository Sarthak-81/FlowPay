package com.flowpay.FlowPay.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO returned by the order endpoints.
 *
 * <p>A clean, flat representation of an {@link com.flowpay.FlowPay.entity.Order}
 * without the bidirectional JPA relationship that causes infinite JSON recursion.
 * The {@code items} list contains {@link OrderItemResponse} objects which do NOT
 * hold a back-reference to this order.</p>
 */
@Getter
@Setter
@Builder
public class OrderResponse {
    private Long id;
    private Double amount;
    private String status;
    private String userEmail;
    private LocalDateTime createdAt;
    private String razorpayOrderId;
    private List<OrderItemResponse> items;
}
