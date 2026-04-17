package com.nhom611.usersvc.service;

import com.nhom611.usersvc.domain.RefreshToken;
import com.nhom611.usersvc.domain.User;
import com.nhom611.usersvc.domain.UserRole;
import com.nhom611.usersvc.domain.UserStatus;
import com.nhom611.usersvc.dto.AuthDtos;
import com.nhom611.usersvc.repository.RefreshTokenRepository;
import com.nhom611.usersvc.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthDtos.RegisterResponse register(AuthDtos.RegisterRequest req) {
        String email = normalizeEmail(req.email());

        if (req.role() != UserRole.EMPLOYER && req.role() != UserRole.FREELANCER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only EMPLOYER/FREELANCER can self-register");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        Instant now = Instant.now();
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role());
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User saved = userRepository.save(user);
        return new AuthDtos.RegisterResponse(toUserResponse(saved));
    }

    public LoginResult login(AuthDtos.LoginRequest req) {
        String email = normalizeEmail(req.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is banned");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String access = jwtService.issueAccessToken(user);
        RefreshTokenService.IssuedRefreshToken refresh = refreshTokenService.issue(user.getId());

        AuthDtos.AuthResponse body = new AuthDtos.AuthResponse(access, jwtService.accessTtlSeconds(), toUserResponse(user));
        return new LoginResult(body, refresh.rawToken());
    }

    public RefreshResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
        }

        String hash = refreshTokenService.hash(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenService.revoke(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (user.getStatus() == UserStatus.BANNED) {
            refreshTokenService.revoke(token);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is banned");
        }

        refreshTokenService.revoke(token);
        String access = jwtService.issueAccessToken(user);
        RefreshTokenService.IssuedRefreshToken newRefresh = refreshTokenService.issue(user.getId());
        AuthDtos.AuthResponse body = new AuthDtos.AuthResponse(access, jwtService.accessTtlSeconds(), toUserResponse(user));

        return new RefreshResult(body, newRefresh.rawToken());
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String hash = refreshTokenService.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash).ifPresent(refreshTokenService::revoke);
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static AuthDtos.UserResponse toUserResponse(User user) {
        return new AuthDtos.UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getStatus());
    }

    public record LoginResult(AuthDtos.AuthResponse response, String refreshToken) {
    }

    public record RefreshResult(AuthDtos.AuthResponse response, String refreshToken) {
    }
}
