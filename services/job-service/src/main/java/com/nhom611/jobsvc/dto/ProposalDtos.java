package com.nhom611.jobsvc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nhom611.jobsvc.domain.ProposalStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ProposalDtos {

    private ProposalDtos() {
    }

    public record SubmitProposalRequest(
            @NotNull @DecimalMin("0.01") BigDecimal bidAmount,
            @NotBlank @Size(min = 10, max = 500) String message
    ) {
    }

    public record ProposalResponse(
            String id,
            String jobId,
            String jobTitle,
            String freelancerId,
            BigDecimal bidAmount,
            String message,
            ProposalStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant respondedAt
    ) {
    }

    public record ProposalDetailResponse(
            String id,
            String jobId,
            String jobTitle,
            String freelancerId,
            BigDecimal bidAmount,
            String message,
            ProposalStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant respondedAt
    ) {
    }
}
