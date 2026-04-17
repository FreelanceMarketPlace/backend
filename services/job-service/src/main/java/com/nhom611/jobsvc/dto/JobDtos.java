package com.nhom611.jobsvc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nhom611.jobsvc.domain.BudgetType;
import com.nhom611.jobsvc.domain.JobStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class JobDtos {

    private JobDtos() {
    }

    public enum Sort {
        NEWEST,
        BUDGET_ASC,
        BUDGET_DESC
    }

    public record CreateJobRequest(
            @NotBlank @Size(min = 10, max = 100) String title,
            @NotBlank @Size(min = 50, max = 20000) String description,
            @Size(max = 10) List<@NotBlank String> requiredSkills,
            @NotNull BudgetType budgetType,
            @DecimalMin("0.01") BigDecimal fixedBudget,
            @DecimalMin("0.01") BigDecimal hourlyRate,
            Integer estimatedHours,
            Instant deadline
    ) {
    }

    public record UpdateJobRequest(
            @Size(min = 10, max = 100) String title,
            @Size(min = 50, max = 20000) String description,
            @Size(max = 10) List<@NotBlank String> requiredSkills,
            BudgetType budgetType,
            @DecimalMin("0.01") BigDecimal fixedBudget,
            @DecimalMin("0.01") BigDecimal hourlyRate,
            Integer estimatedHours,
            Instant deadline
    ) {
    }

    public record JobListItemResponse(
            String id,
            String employerId,
            String title,
            List<String> requiredSkills,
            BudgetType budgetType,
            BigDecimal fixedBudget,
            BigDecimal hourlyRate,
            Integer estimatedHours,
            Instant deadline,
            JobStatus status,
            int proposalCount,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record JobDetailResponse(
            String id,
            String employerId,
            String title,
            String description,
            List<String> requiredSkills,
            BudgetType budgetType,
            BigDecimal fixedBudget,
            BigDecimal hourlyRate,
            Integer estimatedHours,
            Instant deadline,
            JobStatus status,
            int proposalCount,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
