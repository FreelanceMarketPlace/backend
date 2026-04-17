package com.nhom611.usersvc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nhom611.usersvc.domain.UserRole;
import com.nhom611.usersvc.domain.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull UserRole role
    ) {
    }

    public record RegisterResponse(UserResponse user) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {
    }

    public record AuthResponse(
            String accessToken,
            long expiresInSeconds,
            UserResponse user
    ) {
    }

    public record UserResponse(
            String id,
            String email,
            UserRole role,
            UserStatus status
    ) {
    }
}
