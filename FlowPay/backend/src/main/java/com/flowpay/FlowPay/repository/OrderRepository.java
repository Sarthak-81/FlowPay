package com.flowpay.FlowPay.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flowpay.FlowPay.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>
{
    List<Order> findByUserEmail(String email);

    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);
}
