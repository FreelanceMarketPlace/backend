package com.nhom611.contractsvc.dto;

import com.nhom611.contractsvc.domain.ContractStatus;
import com.nhom611.contractsvc.domain.MilestoneStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class ContractDtos {

    // ============ REQUEST DTOs ============

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateContractRequest {
        private String offerId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MilestoneSubmitRequest {
        private String fileUrl;
        private String linkUrl;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevisionRequest {
        private String revisionDesc;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MilestoneApproveRequest {
        private String confirmedByEmployerId;
    }

    // ============ RESPONSE DTOs ============

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContractResponse {
        private String id;
        private String jobId;
        private String offerId;
        private String employerId;
        private String freelancerId;
        private Double totalValue;
        private ContractStatus status;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MilestoneResponse {
        private String id;
        private String contractId;
        private String title;
        private String description;
        private Double amount;
        private LocalDateTime dueDate;
        private MilestoneStatus status;
        private Integer revisionCount;
        private Integer maxRevisions;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeliverableResponse {
        private String id;
        private String milestoneId;
        private String fileUrl;
        private String linkUrl;
        private String description;
        private LocalDateTime submittedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EscrowResponse {
        private String id;
        private String milestoneId;
        private Double amount;
        private Boolean isFrozen;
        private LocalDateTime lockedAt;
        private LocalDateTime releasedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContractDetailResponse {
        private ContractResponse contract;
        private List<MilestoneResponse> milestones;
        private List<DeliverableResponse> deliverables;
        private EscrowResponse escrow;
    }
}
