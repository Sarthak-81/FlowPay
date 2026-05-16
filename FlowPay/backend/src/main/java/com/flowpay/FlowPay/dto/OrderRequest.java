package com.flowpay.FlowPay.dto;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for the order creation request body.
 *
 * <p>Sent by the frontend when the user initiates a payment.
 * The {@code amount} is in rupees (INR) and will be converted
 * to paise (×100) when calling the Razorpay API.</p>
 */
@Getter
@Setter
public class OrderRequest {

     private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest 
    {
        private String itemName;
        private Integer quantity;
        private Double unitPrice;
    }
}
