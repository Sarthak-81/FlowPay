package com.flowpay.FlowPay.controller;

import com.flowpay.FlowPay.dto.AuthResponse;
import com.flowpay.FlowPay.dto.LoginRequest;
import com.flowpay.FlowPay.dto.RefreshTokenRequest;
import com.flowpay.FlowPay.dto.SignupRequest;
import com.flowpay.FlowPay.service.AuthService;
import com.flowpay.FlowPay.service.RefreshTokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user authentication endpoints.
 *
 * <p>These endpoints are public (no JWT required) as configured in
 * {@link com.flowpay.FlowPay.config.SecurityConfig}. All paths under
 * {@code /auth/**} bypass the {@link com.flowpay.FlowPay.config.JwtFilter}.</p>
 *
 * <p>Base URL: {@code /auth}</p>
 *
 * <h3>Error responses</h3>
 * <p>All errors are returned as structured JSON by
 * {@link com.flowpay.FlowPay.exception.GlobalExceptionHandler}:</p>
 * <pre>{@code
 * {
 *   "timestamp": "2025-01-01T10:00:00",
 *   "status":    409,
 *   "error":     "Conflict",
 *   "message":   "Email already registered: alice@example.com",
 *   "path":      "/auth/signup"
 * }
 * }</pre>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    /**
     * Registers a new user account.
     *
     * <p>The request body must contain {@code name}, {@code email}, and {@code password}
     * (plain text — hashed server-side with BCrypt before storage).</p>
     *
     * <h3>Request</h3>
     * <pre>{@code
     * POST /auth/signup
     * Content-Type: application/json
     *
     * { "name": "Alice", "email": "alice@example.com", "password": "secret123" }
     * }</pre>
     *
     * <h3>Response (200 OK)</h3>
     * <pre>{@code { "message": "User registered successfully" } }</pre>
     *
     * <h3>Response (409 Conflict)</h3>
     * <p>Returned when the email is already registered.</p>
     *
     * <p><b>Fix:</b> {@code AuthService.signup} previously returned a {@code ResponseEntity<?>},
     * which caused a double-wrapping bug ({@code ResponseEntity} inside {@code ResponseEntity}).
     * The service now returns a plain {@code Map<String, String>}, so the controller wraps
     * it exactly once with {@code ResponseEntity.ok(...)}.</p>
     *
     * @param request the signup details (name, email, password)
     * @return 200 OK with a success message map
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    /**
     * Authenticates a user and returns a JWT Bearer token.
     *
     * <p>The returned token should be included in all subsequent authenticated
     * requests as: {@code Authorization: Bearer <token>}.</p>
     *
     * <h3>Request</h3>
     * <pre>{@code
     * POST /auth/login
     * Content-Type: application/json
     *
     * { "email": "alice@example.com", "password": "secret123" }
     * }</pre>
     *
     * <h3>Response (200 OK)</h3>
     * <pre>{@code "eyJhbGciOiJIUzI1NiJ9..." }</pre>
     *
     * <h3>Response (404 Not Found)</h3>
     * <p>Returned when no account exists for the given email.</p>
     *
     * <h3>Response (400 Bad Request)</h3>
     * <p>Returned when the password is incorrect.</p>
     *
     * @param request the login credentials (email, password)
     * @return 200 OK with the signed JWT string
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
public ResponseEntity<AuthResponse> refreshToken(
        @RequestBody RefreshTokenRequest request
) {
    String newAccessToken =
            refreshTokenService.validateAndGenerateAccessToken(request.getRefreshToken());

    return ResponseEntity.ok(
            AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(request.getRefreshToken())
                    .tokenType("Bearer")
                    .build()
    );
}

@PostMapping("/logout")
public ResponseEntity<String> logout(
        @RequestBody RefreshTokenRequest request
) {
    refreshTokenService.revokeToken(request.getRefreshToken());
    return ResponseEntity.ok("Logged out successfully");
}
}
