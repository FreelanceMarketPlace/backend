package com.nhom611.jobsvc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nhom611.jobsvc.domain.ProposalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ProposalDtos {

    private ProposalDtos() {
    }

    public record SubmitProposalRequest(
            @NotBlank @Size(min = 10, max = 500) String coverLetter,
            @NotNull @jakarta.validation.constraints.Positive Integer estimatedDuration
    ) {
    }

    public record ProposalResponse(
            String id,
            String jobId,
            String jobTitle,
            String freelancerId,
            String coverLetter,
            Integer estimatedDuration,
            ProposalStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant respondedAt,
            List<ProposalAttachmentResponse> attachments
    ) {
    }

    public record ProposalDetailResponse(
            String id,
            String jobId,
            String jobTitle,
            String freelancerId,
            String coverLetter,
            Integer estimatedDuration,
            ProposalStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant respondedAt,
            List<ProposalAttachmentResponse> attachments
    ) {
    }

    public record ProposalAttachmentResponse(
            String attachmentId,
            String fileName,
            String mimeType,
            Long fileSize,
            Instant uploadedAt,
            String downloadUrl
    ) {
    }
}
