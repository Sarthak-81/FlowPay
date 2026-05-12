package com.flowpay.FlowPay.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.flowpay.FlowPay.entity.PaymentTransaction;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByRazorpayOrderId(String razorpayOrderId);

    Optional<PaymentTransaction> findByRazorpayPaymentId(String razorpayPaymentId);
}
