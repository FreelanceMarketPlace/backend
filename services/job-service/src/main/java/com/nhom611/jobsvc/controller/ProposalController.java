package com.nhom611.jobsvc.controller;

import com.nhom611.jobsvc.domain.ProposalStatus;
import com.nhom611.jobsvc.dto.ProposalDtos;
import com.nhom611.jobsvc.service.ProposalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class    ProposalController {

    private static final Logger log = LoggerFactory.getLogger(ProposalController.class);

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    /**
     * POST /jobs/{jobId}/proposals - Submit a proposal for a job (Freelancer)
     */
    @PostMapping("/jobs/{jobId}/proposals")
    public ResponseEntity<ProposalDtos.ProposalResponse> submitProposal(
            @PathVariable String jobId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProposalDtos.SubmitProposalRequest req
    ) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        log.info("Received proposal submit request jobId={}, freelancerId={}", jobId, jwt.getSubject());
        ProposalDtos.ProposalResponse response = proposalService.submitProposal(jobId, jwt.getSubject(), req);
        log.info("Proposal submit completed jobId={}, freelancerId={}, proposalId={}", jobId, jwt.getSubject(), response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /jobs/{jobId}/proposals - Get all proposals for a job (Employer only)
     */
    @GetMapping("/jobs/{jobId}/proposals")
    public ResponseEntity<Map<String, Object>> getJobProposals(
            @PathVariable String jobId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) ProposalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 100));
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        Page<ProposalDtos.ProposalResponse> proposals = proposalService.getJobProposals(jobId, jwt.getSubject(), status, pageable);

        return ResponseEntity.ok(Map.of(
                "items", proposals.getContent(),
                "page", proposals.getNumber(),
                "size", proposals.getSize(),
                "totalElements", proposals.getTotalElements(),
                "totalPages", proposals.getTotalPages()
        ));
    }

    /**
     * GET /freelancer/proposals - Get all proposals by freel   ancer
     */
    @GetMapping("/freelancer/proposals")
    public ResponseEntity<Map<String, Object>> getFreelancerProposals(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) ProposalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 100));
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        Page<ProposalDtos.ProposalResponse> proposals = proposalService.getFreelancerProposals(jwt.getSubject(), status, pageable);

        return ResponseEntity.ok(Map.of(
                "items", proposals.getContent(),
                "page", proposals.getNumber(),
                "size", proposals.getSize(),
                "totalElements", proposals.getTotalElements(),
                "totalPages", proposals.getTotalPages()
        ));
    }

    /**
     * POST /proposals/{proposalId}/shortlist - Shortlist a proposal (Employer only)
     */
    @PostMapping("/proposals/{proposalId}/shortlist")
    public ResponseEntity<ProposalDtos.ProposalResponse> shortlistProposal(
            @PathVariable String proposalId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        ProposalDtos.ProposalResponse response = proposalService.shortlistProposal(proposalId, jwt.getSubject());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /proposals/{proposalId}/reject - Reject a proposal (Employer only)
     */
    @PostMapping("/proposals/{proposalId}/reject")
    public ResponseEntity<ProposalDtos.ProposalResponse> rejectProposal(
            @PathVariable String proposalId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        ProposalDtos.ProposalResponse response = proposalService.rejectProposal(proposalId, jwt.getSubject());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /proposals/{proposalId}/withdraw - Withdraw a proposal (Freelancer only)
     */
    @PostMapping("/proposals/{proposalId}/withdraw")
    public ResponseEntity<ProposalDtos.ProposalResponse> withdrawProposal(
            @PathVariable String proposalId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        ProposalDtos.ProposalResponse response = proposalService.withdrawProposal(proposalId, jwt.getSubject());
        return ResponseEntity.ok(response);
    }
}
