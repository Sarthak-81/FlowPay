package com.flowpay.FlowPay.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo/smoke-test controller for verifying role-based access control.
 *
 * <p>These endpoints exist purely to confirm that the Spring Security
 * configuration ({@link com.flowpay.FlowPay.config.SecurityConfig}) correctly
 * enforces the {@code USER} and {@code ADMIN} roles. They are <b>not</b> meant
 * for production use and should be removed or protected before going live.</p>
 *
 * <p>Base URL: {@code /api}</p>
 *
 * <h3>Authorization rules</h3>
 * <ul>
 *   <li>{@code GET /api/user}  – accessible by any authenticated user
 *       ({@code ROLE_USER} or {@code ROLE_ADMIN}).</li>
 *   <li>{@code GET /api/admin} – accessible only by users with {@code ROLE_ADMIN}.</li>
 * </ul>
 *
 * <p>Both endpoints require a valid JWT Bearer token in the
 * {@code Authorization} header.</p>
 */
@RestController
@RequestMapping("/api")
public class DemoController {

    /**
     * Smoke-test endpoint accessible by any authenticated user.
     *
     * <h3>Request</h3>
     * <pre>{@code
     * GET /api/user
     * Authorization: Bearer <jwt>
     * }</pre>
     *
     * <h3>Response (200 OK)</h3>
     * <pre>{@code "User API" }</pre>
     *
     * @return a plain-text confirmation string
     */
    @GetMapping("/user")
    public String userApi() {
        return "User API";
    }

    /**
     * Smoke-test endpoint restricted to administrators only.
     *
     * <p>Access requires the {@code ROLE_ADMIN} authority, as configured in
     * {@link com.flowpay.FlowPay.config.SecurityConfig}. Requests from regular
     * {@code ROLE_USER} accounts will receive a <b>403 Forbidden</b> response.</p>
     *
     * <h3>Request</h3>
     * <pre>{@code
     * GET /api/admin
     * Authorization: Bearer <jwt-with-admin-role>
     * }</pre>
     *
     * <h3>Response (200 OK)</h3>
     * <pre>{@code "Admin API" }</pre>
     *
     * <h3>Response (403 Forbidden)</h3>
     * <p>Returned automatically by Spring Security when the authenticated user
     * does not have the {@code ROLE_ADMIN} authority.</p>
     *
     * @return a plain-text confirmation string
     */
    @GetMapping("/admin")
    public String adminApi() {
        return "Admin API";
    }
}
