package com.flowpay.FlowPay.utility;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET =
            "replace-this-with-your-long-secret-key-at-least-32-characters";

    private final long accessTokenExpirationMs = 1000 * 60 * 60; // 1 hour

    private final long refreshTokenExpirationMs = 1000L * 60 * 60 * 24 * 7; // 7 days

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "ACCESS")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "REFRESH")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractTokenType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean isTokenValid(String token, String email) {
        return extractUsername(token).equals(email)
                && !isTokenExpired(token)
                && "ACCESS".equals(extractTokenType(token));
    }

    public boolean isRefreshTokenValid(String token, String email) {
        return extractUsername(token).equals(email)
                && !isTokenExpired(token)
                && "REFRESH".equals(extractTokenType(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}