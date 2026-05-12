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

/**
 * REST controller for user authentication endpoints.
 *
 * <p>These endpoints are public (no JWT required) as configured in
 * {@link com.flowpay.FlowPay.config.SecurityConfig}.</p>
 *
 * <p>Base URL: {@code /auth}</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Registers a new user account.
     *
     * <p>Request body must contain {@code name}, {@code email}, and {@code password}.</p>
     *
     * @param request the signup details
     * @return 200 OK with a success message, or an error if the email is already taken
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    /**
     * Authenticates a user and returns a JWT Bearer token.
     *
     * <p>Request body must contain {@code email} and {@code password}.
     * The returned token should be included in subsequent requests as:
     * {@code Authorization: Bearer <token>}.</p>
     *
     * @param request the login credentials
     * @return 200 OK with the JWT string, or an error if credentials are invalid
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
