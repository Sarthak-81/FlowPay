package com.flowpay.FlowPay.repository;

import com.flowpay.FlowPay.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
}