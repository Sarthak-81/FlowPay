package com.flowpay.FlowPay.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.flowpay.FlowPay.entity.User;
import com.flowpay.FlowPay.repository.UserRepository;
import com.flowpay.FlowPay.utility.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet filter that validates JWT Bearer tokens on every incoming request.
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee exactly one execution per
 * request, even in filter chain setups that might otherwise invoke it multiple times.</p>
 *
 * <h3>Processing flow:</h3>
 * <ol>
 *   <li>Extract the {@code Authorization} header and check for the {@code Bearer } prefix.</li>
 *   <li>Parse the JWT to extract the user's email via {@link JwtUtil#extractEmail}.</li>
 *   <li>Load the {@link User} entity from the database using the email.</li>
 *   <li>Build a {@link UsernamePasswordAuthenticationToken} with the user's role and
 *       set it on the {@link SecurityContextHolder}.</li>
 *   <li>Continue the filter chain regardless (unauthenticated requests are rejected
 *       by {@link SecurityConfig} rules downstream).</li>
 * </ol>
 *
 * <p>Auth and OPTIONS requests are skipped via {@link #shouldNotFilter}.</p>
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    /**
     * Core filter logic: validates the JWT and populates the security context.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();

        if (path.equals("/api/payments/webhook")) 
        {
            filterChain.doFilter(request, response);
            return;
        }        
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);

            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority(user.getRole()))
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Skips JWT filtering for public auth endpoints and CORS preflight requests.
     *
     * @param request the incoming HTTP request
     * @return {@code true} if the filter should be bypassed for this request
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.startsWith("/auth/")
                || path.startsWith("/api/auth/")
            || path.equals("/api/payments/webhook")
            || request.getMethod().equalsIgnoreCase("OPTIONS");
    }
}
