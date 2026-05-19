package com.flowpay.FlowPay.service;

import com.flowpay.FlowPay.entity.RefreshToken;
import com.flowpay.FlowPay.repository.RefreshTokenRepository;
import com.flowpay.FlowPay.utility.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    public String createRefreshToken(String email) {

        String token = jwtUtil.generateRefreshToken(email);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .userEmail(email)
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    public String validateAndGenerateAccessToken(String refreshTokenValue) {

        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (Boolean.TRUE.equals(savedToken.getRevoked())) {
            throw new RuntimeException("Refresh token is revoked");
        }

        if (savedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        String email = savedToken.getUserEmail();

        if (!jwtUtil.isRefreshTokenValid(refreshTokenValue, email)) {
            throw new RuntimeException("Invalid refresh token");
        }

        return jwtUtil.generateAccessToken(email);
    }

    public void revokeToken(String refreshTokenValue) {

        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }
}