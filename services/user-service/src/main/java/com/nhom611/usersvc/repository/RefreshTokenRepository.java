package com.nhom611.usersvc.repository;

import com.nhom611.usersvc.domain.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);
    long deleteByExpiresAtBefore(Instant cutoff);
}
