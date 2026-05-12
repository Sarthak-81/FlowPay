package com.flowpay.FlowPay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the FlowPay Spring Boot application.
 *
 * <p>FlowPay is a payment processing backend that integrates with Razorpay
 * to handle order creation, payment verification, and webhook processing.
 * It uses JWT-based authentication and a MySQL database.</p>
 *
 * <p>Start the application using:</p>
 * <pre>./mvnw spring-boot:run</pre>
 */
@SpringBootApplication
public class FlowPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowPayApplication.class, args);
    }
}
