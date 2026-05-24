package com.nhom611.contractsvc.controller;

import com.nhom611.contractsvc.dto.ContractDtos;
import com.nhom611.contractsvc.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/milestones")
@RequiredArgsConstructor
public class MilestoneController {
    
    private final MilestoneService milestoneService;

    /**
     * Freelancer submit deliverable cho milestone
     */
    @PostMapping("/{milestoneId}/submit")
    public ResponseEntity<ContractDtos.DeliverableResponse> submitMilestone(
            @PathVariable String milestoneId,
            @RequestBody ContractDtos.MilestoneSubmitRequest request) {
        ContractDtos.DeliverableResponse response = milestoneService.submitMilestone(milestoneId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Employer request revision cho milestone
     */
    @PostMapping("/{milestoneId}/revision")
    public ResponseEntity<ContractDtos.MilestoneResponse> requestRevision(
            @PathVariable String milestoneId,
            @RequestBody ContractDtos.RevisionRequest request) {
        ContractDtos.MilestoneResponse response = milestoneService.requestRevision(milestoneId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Employer approve milestone
     */
    @PostMapping("/{milestoneId}/approve")
    public ResponseEntity<ContractDtos.MilestoneResponse> approveMilestone(
            @PathVariable String milestoneId) {
        ContractDtos.MilestoneResponse response = milestoneService.approveMilestone(milestoneId);
        return ResponseEntity.ok(response);
    }
}
