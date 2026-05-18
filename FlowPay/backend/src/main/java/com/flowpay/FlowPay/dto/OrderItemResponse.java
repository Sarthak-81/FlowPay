package com.flowpay.FlowPay.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for an individual order item in the order response.
 * Excludes the back-reference to {@code Order} so serialization stays flat.
 */
@Getter
@Setter
@Builder
public class OrderItemResponse {
    private Long id;
    private String itemName;
    private Integer quantity;
    private Double unitPrice;
    private Double totalPrice;
}
