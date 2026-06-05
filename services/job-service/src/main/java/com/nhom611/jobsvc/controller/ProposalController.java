package com.nhom611.jobsvc.controller;

import com.nhom611.jobsvc.domain.ProposalStatus;
import com.nhom611.jobsvc.dto.ProposalDtos;
import com.nhom611.jobsvc.service.ProposalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
public class ProposalController {

    private static final Logger log = LoggerFactory.getLogger(ProposalController.class);

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    /**
     * POST /jobs/{jobId}/proposals - Submit a proposal for a job (Freelancer)
     */
    @PostMapping(value = "/jobs/{jobId}/proposals", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProposalDtos.ProposalResponse> submitProposal(
            @PathVariable String jobId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("proposal") @Valid ProposalDtos.SubmitProposalRequest req,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
            HttpServletRequest request
    ) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String clientIp = resolveClientIp(request);
        log.info("Received proposal submit request jobId={}, freelancerId={}, clientIp={}", jobId, jwt.getSubject(), clientIp);
        ProposalDtos.ProposalResponse response = proposalService.submitProposal(jobId, jwt.getSubject(), req, attachments, clientIp);
        log.info("Proposal submit completed jobId={}, freelancerId={}, proposalId={}", jobId, jwt.getSubject(), response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
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

    @GetMapping("/proposals/{proposalId}/attachments/{attachmentId}/download")
    public ResponseEntity<ByteArrayResource> downloadAttachment(
            @PathVariable String proposalId,
            @PathVariable String attachmentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }

        var storedFile = proposalService.downloadStoredAttachment(proposalId, attachmentId, jwt.getSubject());
        var resource = new ByteArrayResource(storedFile.content());
        String contentType = storedFile.contentType() == null ? MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE : storedFile.contentType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(storedFile.content().length)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(storedFile.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }
}
