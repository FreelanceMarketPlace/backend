package com.nhom611.usersvc.service;

import com.nhom611.usersvc.config.AuthProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class RefreshCookieService {

    private final AuthProperties properties;

    public RefreshCookieService(AuthProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie buildRefreshCookie(String rawRefreshToken) {
        AuthProperties.Refresh.Cookie c = properties.getRefresh().getCookie();
        return ResponseCookie.from(c.getName(), rawRefreshToken)
                .httpOnly(true)
                .secure(c.isSecure())
                .path(c.getPath())
                .sameSite(c.getSameSite())
                .maxAge(properties.getRefresh().getTtlSeconds())
                .build();
    }

    public ResponseCookie clearRefreshCookie() {
        AuthProperties.Refresh.Cookie c = properties.getRefresh().getCookie();
        return ResponseCookie.from(c.getName(), "")
                .httpOnly(true)
                .secure(c.isSecure())
                .path(c.getPath())
                .sameSite(c.getSameSite())
                .maxAge(0)
                .build();
    }
}
