package com.flowpay.FlowPay.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentSuccessEvent 
{
    private String orderId;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private Double amount;

    private String status;

    private LocalDateTime eventTime;
}