package com.nhom611.usersvc.service;

import com.nhom611.usersvc.config.AuthProperties;
import com.nhom611.usersvc.domain.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final AuthProperties properties;

    public JwtService(JwtEncoder jwtEncoder, AuthProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getJwt().getAccessTtlSeconds());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("user-service")
                .issuedAt(now)
                .expiresAt(exp)
                .subject(user.getId())
                .id(UUID.randomUUID().toString())
                .claim("role", user.getRole().name())
                .build();

            JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
            return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long accessTtlSeconds() {
        return properties.getJwt().getAccessTtlSeconds();
    }
}
