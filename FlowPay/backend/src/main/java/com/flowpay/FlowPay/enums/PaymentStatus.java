package com.flowpay.FlowPay.enums;

/**
 * Enumeration of possible payment statuses for a {@link com.flowpay.FlowPay.entity.PaymentTransaction}.
 *
 * <ul>
 *   <li>{@link #CREATED}  – The order has been created but payment has not yet been initiated.</li>
 *   <li>{@link #SUCCESS}  – Payment was captured successfully by Razorpay.</li>
 *   <li>{@link #FAILED}   – Payment failed or was rejected.</li>
 * </ul>
 */
public enum PaymentStatus {

    /** Order created; awaiting payment initiation from the frontend. */
    CREATED,

    /** Payment successfully captured (confirmed via webhook or signature verification). */
    SUCCESS,

    /** Payment failed or was not completed. */
    FAILED
}
