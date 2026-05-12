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

/**
 * Service layer for user authentication and registration.
 *
 * <p>Handles two operations:</p>
 * <ul>
 *   <li><b>Signup</b>: Validates uniqueness, hashes the password with BCrypt,
 *       and persists a new {@link User}.</li>
 *   <li><b>Login</b>: Verifies credentials and returns a signed JWT on success.</li>
 * </ul>
 */
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Registers a new user account.
     *
     * <p>Throws a {@link RuntimeException} if the email is already registered.
     * The password is encoded using BCrypt before persistence. The default role
     * assigned is {@code ROLE_USER}.</p>
     *
     * @param request the signup request containing name, email, and plain-text password
     * @return a 200 OK response with a success message map
     * @throws RuntimeException if the email is already in use
     */
    public ResponseEntity<?> signup(SignupRequest request) {

        if (userRepository.existsByEmail(request.email)) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.email);
        user.setPassword(passwordEncoder.encode(request.password));
        user.setRole("ROLE_USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setName(request.name);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    /**
     * Authenticates a user and returns a JWT.
     *
     * <p>Validates the email exists and the supplied plain-text password matches
     * the stored BCrypt hash. On success, generates and returns a signed JWT
     * valid for 1 hour.</p>
     *
     * @param request the login request containing email and plain-text password
     * @return a signed JWT string
     * @throws RuntimeException if the user is not found or the password is incorrect
     */
    public String login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return jwtUtil.generateToken(user.getEmail());
    }
}
