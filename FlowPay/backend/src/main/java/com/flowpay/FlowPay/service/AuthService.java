package com.flowpay.FlowPay.service;

import com.flowpay.FlowPay.dto.LoginRequest;
import com.flowpay.FlowPay.dto.SignupRequest;
import com.flowpay.FlowPay.entity.User;
import com.flowpay.FlowPay.exception.DuplicateResourceException;
import com.flowpay.FlowPay.exception.ResourceNotFoundException;
import com.flowpay.FlowPay.repository.UserRepository;
import com.flowpay.FlowPay.utility.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
     * <p>Throws {@link DuplicateResourceException} (→ HTTP 409) if the email is already
     * registered. The password is encoded using BCrypt before persistence.
     * The default role assigned is {@code ROLE_USER}.</p>
     *
     * <p><b>Fix:</b> The original method returned {@code ResponseEntity<?>}, which caused
     * a double-wrapping bug when the controller called {@code ResponseEntity.ok(signup(request))}.
     * The return type is now a plain {@link Map} so the controller can wrap it once.</p>
     *
     * <h3>Request</h3>
     * <pre>{@code
     * POST /auth/signup
     * { "name": "Alice", "email": "alice@example.com", "password": "secret123" }
     * }</pre>
     *
     * <h3>Response (200 OK)</h3>
     * <pre>{@code
     * { "message": "User registered successfully" }
     * }</pre>
     *
     * <h3>Response (409 Conflict)</h3>
     * <pre>{@code
     * { "status": 409, "message": "Email already registered: alice@example.com" }
     * }</pre>
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

        User user = new User();
        user.setEmail(request.email);
        user.setPassword(passwordEncoder.encode(request.password));
        user.setRole("ROLE_USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setName(request.name);

        userRepository.save(user);

        return Map.of("message", "User registered successfully");
    }

    /**
     * Authenticates a user and returns a signed JWT.
     *
     * <p>Validates the email exists and the supplied plain-text password matches
     * the stored BCrypt hash. On success, generates and returns a signed JWT
     * valid for 1 hour (as configured in {@link JwtUtil#generateToken}).</p>
     *
     * <h3>Request</h3>
     * <pre>{@code
     * POST /auth/login
     * { "email": "alice@example.com", "password": "secret123" }
     * }</pre>
     *
     * <h3>Response (200 OK)</h3>
     * <pre>{@code
     * "eyJhbGciOiJIUzI1NiJ9..."
     * }</pre>
     *
     * <h3>Response (404 Not Found)</h3>
     * <pre>{@code
     * { "status": 404, "message": "No account found for email: alice@example.com" }
     * }</pre>
     *
     * <h3>Response (401 Unauthorized)</h3>
     * <pre>{@code
     * { "status": 401, "message": "Incorrect password." }
     * }</pre>
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
            // Use IllegalArgumentException (→ 400) rather than a generic RuntimeException,
            // so the GlobalExceptionHandler can assign the correct HTTP status.
            // Avoid exposing whether the email or password was wrong to prevent user enumeration.
            throw new IllegalArgumentException("Incorrect password.");
        }

        return jwtUtil.generateToken(user.getEmail());
    }
}
