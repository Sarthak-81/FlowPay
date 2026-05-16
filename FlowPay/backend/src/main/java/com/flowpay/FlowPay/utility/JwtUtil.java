package com.flowpay.FlowPay.utility;

import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Utility component for generating and parsing JSON Web Tokens (JWTs).
 *
 * <p>Tokens are signed with HMAC-SHA256 using a symmetric secret key and
 * expire after <b>1 hour</b>. The email address is stored as the JWT subject
 * and used to identify the authenticated user throughout the application.</p>
 *
 * <p><b>Security note:</b> The secret key should be externalised to
 * {@code application.yaml} (or an environment variable / secrets manager)
 * in production rather than being hard-coded.</p>
 */
@Component
public class JwtUtil 
{
    // TODO: Move this to application.yaml and inject via @Value for production use
    private final String SECRET = "mysecretkeymysecretkeymysecretkey";

    /**
     * Generates a signed JWT for the given email address.
     *
     * <p>The token is valid for 1 hour from the time of creation.</p>
     *
     * @param email the authenticated user's email address (stored as JWT subject)
     * @return a compact, URL-safe JWT string
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60)) // 1 hour
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the email (subject) from a signed JWT.
     *
     * <p>Throws a {@link io.jsonwebtoken.JwtException} if the token is expired,
     * malformed, or the signature does not match.</p>
     *
     * @param token the compact JWT string to parse
     * @return the email address stored in the token's subject claim
     */
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
