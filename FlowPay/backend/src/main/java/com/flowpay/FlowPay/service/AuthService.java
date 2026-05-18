package com.flowpay.FlowPay.service;

import com.flowpay.FlowPay.dto.LoginRequest;
import com.flowpay.FlowPay.dto.SignupRequest;
import com.flowpay.FlowPay.entity.User;
import com.flowpay.FlowPay.exception.DuplicateResourceException;
import com.flowpay.FlowPay.exception.ResourceNotFoundException;
import com.flowpay.FlowPay.mapper.UserMapper;
import com.flowpay.FlowPay.repository.UserRepository;
import com.flowpay.FlowPay.utility.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service layer for user authentication and registration.
 *
 * <p>Handles two operations:</p>
 * <ul>
 *   <li><b>Signup</b>: Validates email uniqueness, hashes the password with BCrypt,
 *       and persists a new {@link User}.</li>
 *   <li><b>Login</b>: Verifies credentials and returns a signed JWT on success.</li>
 * </ul>
 *
 * <p>Exceptions thrown here bubble up to {@link com.flowpay.FlowPay.exception.GlobalExceptionHandler},
 * which converts them to the appropriate HTTP response codes.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    /**
     * Registers a new user account.
     *
     * <p>Throws {@link DuplicateResourceException} (→ HTTP 409) if the email is already
     * registered. The password is encoded using BCrypt before persistence.
     * The default role assigned is {@code ROLE_USER}.</p>
     *
     * @param request the signup request containing name, email, and plain-text password
     * @return a map with a {@code "message"} key confirming successful registration
     * @throws DuplicateResourceException if the email is already in use
     */
    public Map<String, String> signup(SignupRequest request) {

        if (userRepository.existsByEmail(request.email)) {
            throw new DuplicateResourceException(
                    "Email already registered: " + request.email);
        }

        // MapStruct maps name and email; password/role/createdAt are set below.
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.password));
        user.setRole("ROLE_USER");
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        return Map.of("message", "User registered successfully");
    }

    /**
     * Authenticates a user and returns a signed JWT.
     *
     * <p>Validates the email exists and the supplied plain-text password matches
     * the stored BCrypt hash. On success, generates and returns a signed JWT.</p>
     *
     * @param request the login request containing email and plain-text password
     * @return a signed JWT string to be used as {@code Authorization: Bearer <token>}
     * @throws ResourceNotFoundException if no account exists for the given email
     * @throws IllegalArgumentException  if the password is incorrect
     */
    public String login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found for email: " + request.email));

        if (!passwordEncoder.matches(request.password, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password.");
        }

        return jwtUtil.generateToken(user.getEmail());
    }
}
