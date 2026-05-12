package com.flowpay.FlowPay.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS (Cross-Origin Resource Sharing) configuration for the FlowPay API.
 *
 * <p>Permits requests from the frontend development server at
 * {@code http://localhost:5173} (the default Vite dev server port).
 * Update {@code allowedOrigins} for staging/production deployments.</p>
 *
 * <p>The produced {@link CorsConfigurationSource} bean is consumed by
 * {@link SecurityConfig} and applied globally to all routes ({@code /**}).</p>
 */
@Configuration
public class CorsConfig {

    /**
     * Defines the CORS policy applied to all API endpoints.
     *
     * <ul>
     *   <li>Credentials (cookies, Authorization headers) are allowed.</li>
     *   <li>All request headers are permitted.</li>
     *   <li>Allowed HTTP methods: GET, POST, PUT, DELETE, OPTIONS.</li>
     * </ul>
     *
     * @return the configured {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
