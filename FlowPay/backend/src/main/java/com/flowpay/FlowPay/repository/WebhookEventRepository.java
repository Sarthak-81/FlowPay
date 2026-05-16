package com.flowpay.FlowPay.repository;

import com.flowpay.FlowPay.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {}