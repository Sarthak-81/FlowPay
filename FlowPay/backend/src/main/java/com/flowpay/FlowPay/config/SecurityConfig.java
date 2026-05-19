package com.flowpay.FlowPay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration for the FlowPay application.
 *
 * <p>Configures a stateless, JWT-based security model:</p>
 * <ul>
 *   <li>CSRF is disabled (stateless REST API; no browser sessions).</li>
 *   <li>Sessions are never created ({@link SessionCreationPolicy#STATELESS}).</li>
 *   <li>The custom {@link JwtFilter} runs before Spring's default
 *       {@link UsernamePasswordAuthenticationFilter}.</li>
 * </ul>
 *
 * <h3>Authorization rules (in order of precedence):</h3>
 * <ol>
 *   <li>All {@code OPTIONS} preflight requests are permitted (for CORS).</li>
 *   <li>{@code /auth/**} endpoints are public (login, signup).</li>
 *   <li>{@code /api/admin} requires the {@code ADMIN} role.</li>
 *   <li>All other {@code /api/**} endpoints require {@code USER} or {@code ADMIN} role.</li>
 *   <li>Everything else requires authentication.</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtFilter jwtFilter, CorsConfigurationSource corsConfigurationSource) {
        this.jwtFilter = jwtFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * Builds and returns the main {@link SecurityFilterChain}.
     *
     * @param http the {@link HttpSecurity} builder provided by Spring
     * @return the configured security filter chain
     * @throws Exception if any configuration step fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/auth/**", "/api/auth/**", "/api/payments/webhook").permitAll()
                .requestMatchers("/api/admin").hasRole("ADMIN")
                .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Password encoder bean using BCrypt hashing with default strength (10 rounds).
     * Used by {@link com.flowpay.FlowPay.service.AuthService} for password
     * encoding and verification.
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
