package com.flowpay.FlowPay.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoController {

    @GetMapping("/user")
    public String userApi() {
        return "User API";
    }

    @GetMapping("/admin")
    public String adminApi() {
        return "Admin API";
    }
}