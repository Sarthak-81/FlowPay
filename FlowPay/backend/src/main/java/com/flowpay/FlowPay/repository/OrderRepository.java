package com.flowpay.FlowPay.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flowpay.FlowPay.entity.Order;

/**
 * Spring Data JPA repository for {@link Order} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} plus two
 * custom query methods derived from the method names by Spring Data.</p>
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Retrieves all orders placed by the given user.
     *
     * @param email the user's email address
     * @return a list of orders; empty list if none found
     */
    List<Order> findByUserEmail(String email);

    /**
     * Looks up a single order by its Razorpay Order ID.
     * Used during payment verification to update the order status.
     *
     * @param razorpayOrderId the Razorpay Order ID (e.g. {@code order_XXXXXXXXXX})
     * @return an {@link Optional} containing the order, or empty if not found
     */
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);
}
