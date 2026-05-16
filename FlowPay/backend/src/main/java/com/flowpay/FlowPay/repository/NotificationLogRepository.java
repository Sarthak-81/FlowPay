package com.flowpay.FlowPay.repository;

import com.flowpay.FlowPay.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {}