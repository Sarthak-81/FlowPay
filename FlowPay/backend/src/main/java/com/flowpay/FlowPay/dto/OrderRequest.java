package com.flowpay.FlowPay.dto;

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

    /**
     * Payment amount in rupees (INR).
     * Must be a positive value greater than 0.
     */
    public Double amount;
}
