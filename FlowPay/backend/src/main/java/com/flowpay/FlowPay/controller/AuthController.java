package com.flowpay.FlowPay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flowpay.FlowPay.dto.LoginRequest;
import com.flowpay.FlowPay.dto.SignupRequest;
import com.flowpay.FlowPay.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController 
{
    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) 
    {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) 
    {
        return ResponseEntity.ok(authService.login(request));
    }

}