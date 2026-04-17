package com.nhom611.usersvc.controller;

import com.nhom611.usersvc.config.AuthProperties;
import com.nhom611.usersvc.dto.AuthDtos;
import com.nhom611.usersvc.repository.UserRepository;
import com.nhom611.usersvc.service.AuthService;
import com.nhom611.usersvc.service.RefreshCookieService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieService refreshCookieService;
    private final AuthProperties properties;
    private final UserRepository userRepository;

    public AuthController(
            AuthService authService,
            RefreshCookieService refreshCookieService,
            AuthProperties properties,
            UserRepository userRepository
    ) {
        this.authService = authService;
        this.refreshCookieService = refreshCookieService;
        this.properties = properties;
        this.userRepository = userRepository;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthDtos.RegisterResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        AuthDtos.RegisterResponse res = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        AuthService.LoginResult result = authService.login(req);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieService.buildRefreshCookie(result.refreshToken()).toString())
                .body(result.response());
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<AuthDtos.AuthResponse> refresh(HttpServletRequest request) {
        String token = readCookie(request, properties.getRefresh().getCookie().getName());
        AuthService.RefreshResult result = authService.refresh(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieService.buildRefreshCookie(result.refreshToken()).toString())
                .body(result.response());
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = readCookie(request, properties.getRefresh().getCookie().getName());
        authService.logout(token);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookieService.clearRefreshCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return Map.of("authenticated", false);
        }
        String userId = jwt.getSubject();
        return userRepository.findById(userId)
                .<Map<String, Object>>map(u -> Map.of(
                        "authenticated", true,
                        "user", new AuthDtos.UserResponse(u.getId(), u.getEmail(), u.getRole(), u.getStatus())
                ))
                .orElseGet(() -> Map.of(
                        "authenticated", true,
                        "userId", userId,
                        "note", "User not found (token subject)"
                ));
    }

    private static String readCookie(HttpServletRequest request, String cookieName) {
        if (request == null || cookieName == null) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (cookieName.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
