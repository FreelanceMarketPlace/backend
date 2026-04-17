package com.nhom611.jobsvc.controller;

import com.nhom611.jobsvc.domain.BudgetType;
import com.nhom611.jobsvc.domain.JobStatus;
import com.nhom611.jobsvc.dto.JobDtos;
import com.nhom611.jobsvc.service.JobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/jobs")
    public JobDtos.JobDetailResponse createJob(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody JobDtos.CreateJobRequest req
    ) {
        return jobService.createJob(jwt.getSubject(), req);
    }

    @GetMapping("/jobs")
    public Map<String, Object> listJobs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) BudgetType budgetType,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget,
            @RequestParam(required = false) Instant deadlineBefore,
            @RequestParam(defaultValue = "NEWEST") JobDtos.Sort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 100));
        List<String> normalizedSkills = normalizeSkillsQueryParam(skills);

        Page<JobDtos.JobListItemResponse> p = jobService.listPublicJobs(
                q,
                normalizedSkills,
                budgetType,
                minBudget,
                maxBudget,
                deadlineBefore,
                sort,
                pageable
        );

        return Map.of(
                "items", p.getContent(),
                "page", p.getNumber(),
                "size", p.getSize(),
                "totalElements", p.getTotalElements(),
                "totalPages", p.getTotalPages()
        );
    }

    @GetMapping("/jobs/{jobId}")
    public JobDtos.JobDetailResponse getJob(@PathVariable String jobId) {
        return jobService.getJob(jobId);
    }

    @GetMapping("/employer/jobs")
    public Map<String, Object> listEmployerJobs(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 100));
        Page<JobDtos.JobListItemResponse> p = jobService.listEmployerJobs(jwt.getSubject(), status, pageable);

        return Map.of(
                "items", p.getContent(),
                "page", p.getNumber(),
                "size", p.getSize(),
                "totalElements", p.getTotalElements(),
                "totalPages", p.getTotalPages()
        );
    }

    @PatchMapping("/jobs/{jobId}")
    public JobDtos.JobDetailResponse updateJob(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String jobId,
            @Valid @RequestBody JobDtos.UpdateJobRequest req
    ) {
        return jobService.updateJob(jwt.getSubject(), jobId, req);
    }

    @PostMapping("/jobs/{jobId}/close")
    public JobDtos.JobDetailResponse closeJob(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String jobId
    ) {
        return jobService.closeJob(jwt.getSubject(), jobId);
    }

    private static List<String> normalizeSkillsQueryParam(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }

        // Supports both ?skills=java&skills=spring and ?skills=java,spring
        List<String> out = new ArrayList<>();
        for (String token : skills) {
            if (token == null) {
                continue;
            }
            String trimmed = token.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (trimmed.contains(",")) {
                for (String part : trimmed.split(",")) {
                    String p = part.trim();
                    if (!p.isBlank()) {
                        out.add(p);
                    }
                }
            } else {
                out.add(trimmed);
            }
        }
        return out;
    }
}
