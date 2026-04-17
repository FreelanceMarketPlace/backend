package com.nhom611.usersvc.service;

import com.nhom611.usersvc.config.AuthProperties;
import com.nhom611.usersvc.domain.RefreshToken;
import com.nhom611.usersvc.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, AuthProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
    }

    public IssuedRefreshToken issue(String userId) {
        String raw = generateRawToken();
        String hash = sha256Base64Url(raw);

        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(hash);
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(properties.getRefresh().getTtlSeconds()));
        refreshTokenRepository.save(token);

        return new IssuedRefreshToken(raw, token);
    }

    public String hash(String rawToken) {
        return sha256Base64Url(rawToken);
    }

    @Transactional
    public void revoke(RefreshToken token) {
        if (token.getRevokedAt() != null) {
            return;
        }
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Base64Url(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash refresh token", e);
        }
    }

    public record IssuedRefreshToken(String rawToken, RefreshToken persisted) {
    }
}
