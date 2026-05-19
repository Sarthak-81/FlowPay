package com.flowpay.FlowPay.repository;

import com.flowpay.FlowPay.entity.FailedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedEventRepository extends JpaRepository<FailedEvent, Long> {

    List<FailedEvent> findByStatus(String status);
}