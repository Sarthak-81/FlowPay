package com.flowpay.FlowPay.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flowpay.FlowPay.entity.PaymentTransaction;

/**
 * Spring Data JPA repository for {@link PaymentTransaction} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} plus a
 * custom query method for looking up transactions by Razorpay Order ID.</p>
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> 
{
    /**
     * Retrieves a payment transaction by its associated Razorpay Order ID.
     * Used by the webhook handler to locate the transaction to update.
     *
     * @param razorpayOrderId the Razorpay Order ID (e.g. {@code order_XXXXXXXXXX})
     * @return an {@link Optional} containing the transaction, or empty if not found
     */
    Optional<PaymentTransaction> findByRazorpayOrderId(String razorpayOrderId);
}
