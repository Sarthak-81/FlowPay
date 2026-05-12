package com.flowpay.FlowPay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class RazorpayWebhookRequest 
{
    private String event;

    private Payload payload;

    @Data
    public static class Payload {
        private PaymentEntity payment;
    }

    @Data
    public static class PaymentEntity {
        private Entity entity;
    }

    @Data
    public static class Entity {
        private String id;
        
        @JsonProperty("order_id")
        private String order_id;

        private String status;

        private Integer amount;
    }
}