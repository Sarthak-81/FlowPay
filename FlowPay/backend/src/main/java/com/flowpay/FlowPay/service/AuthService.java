package com.flowpay.FlowPay.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.flowpay.FlowPay.dto.LoginRequest;
import com.flowpay.FlowPay.dto.SignupRequest;
import com.flowpay.FlowPay.entity.User;
import com.flowpay.FlowPay.repository.UserRepository;
import com.flowpay.FlowPay.utility.JwtUtil;

@Service
public class AuthService 
{

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public ResponseEntity<?> signup(SignupRequest request) 
    {

        if (userRepository.existsByEmail(request.email)) 
            throw new RuntimeException("Email already exists");
        
        User user = new User();
        user.setEmail(request.email);
        user.setPassword(passwordEncoder.encode(request.password));
        user.setRole("ROLE_USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setName(request.name);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    public String login(LoginRequest request) 
    {
        User user = userRepository.findByEmail(request.email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.password, user.getPassword()))
            throw new RuntimeException("Invalid password");
        
        return jwtUtil.generateToken(user.getEmail());
    }
}