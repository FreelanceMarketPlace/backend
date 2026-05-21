package com.nhom611.jobsvc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nhom611.jobsvc.domain.OfferStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class OfferDtos {

    private OfferDtos() {
    }

    public record CreateOfferRequest(
            @NotNull @Positive Integer estimatedDuration,
            @NotNull String jobDescription,
            Instant expiresAt
    ) {
    }

    public record OfferResponse(
            String offerId,
            String jobId,
            String proposalId,
            String employerId,
            String freelancerId,
            String jobDescription,
            BigDecimal contractValue,
            Integer estimatedDuration,
            OfferStatus status,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt,
            Instant respondedAt
    ) {
    }
}