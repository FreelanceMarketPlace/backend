package com.nhom611.jobsvc.service;

import com.nhom611.jobsvc.domain.Job;
import com.nhom611.jobsvc.domain.JobStatus;
import com.nhom611.jobsvc.domain.ProposalAttachment;
import com.nhom611.jobsvc.domain.Proposal;
import com.nhom611.jobsvc.domain.ProposalStatus;
import com.nhom611.jobsvc.dto.ProposalDtos;
import com.nhom611.jobsvc.repository.JobRepository;
import com.nhom611.jobsvc.repository.ProposalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProposalService {

    private static final Logger log = LoggerFactory.getLogger(ProposalService.class);

    private static final long MAX_ATTACHMENT_SIZE_BYTES = 20L * 1024 * 1024;
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final ProposalRepository proposalRepository;
    private final JobRepository jobRepository;
    private final ProposalFileStorageService proposalFileStorageService;
    private final ProposalRateLimitService proposalRateLimitService;

    public ProposalService(ProposalRepository proposalRepository, JobRepository jobRepository, ProposalFileStorageService proposalFileStorageService, ProposalRateLimitService proposalRateLimitService) {
        this.proposalRepository = proposalRepository;
        this.jobRepository = jobRepository;
        this.proposalFileStorageService = proposalFileStorageService;
        this.proposalRateLimitService = proposalRateLimitService;
    }

    /**
     * Submit a proposal for a job
     */
    public ProposalDtos.ProposalResponse submitProposal(String jobId, String freelancerId, ProposalDtos.SubmitProposalRequest req, List<MultipartFile> files, String clientIp) {
        log.info("Submitting proposal jobId={}, freelancerId={}, clientIp={}", jobId, freelancerId, clientIp);
        try {
            proposalRateLimitService.enforceClientIpLimit(clientIp);
            proposalRateLimitService.enforceFreelancerLimit(freelancerId);

            // Check if job exists and is open
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

            if (job.getStatus() != JobStatus.OPEN) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job is not open for proposals");
            }

            // Check if freelancer already proposed for this job
            if (proposalRepository.existsByJobIdAndFreelancerId(jobId, freelancerId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You already proposed for this job");
            }

            // Create proposal
            Proposal proposal = new Proposal();
            proposal.setId(UUID.randomUUID().toString());
            proposal.setJobId(jobId);
            proposal.setFreelancerId(freelancerId);
            proposal.setJobTitle(job.getTitle());
            proposal.setCoverLetter(req.coverLetter());
            proposal.setEstimatedDuration(req.estimatedDuration());
            proposal.setStatus(ProposalStatus.PENDING);
            proposal.setCreatedAt(Instant.now());
            proposal.setUpdatedAt(Instant.now());
            try {
                proposal.setAttachments(uploadAttachments(proposal.getId(), files));
            } catch (IOException ex) {
                log.error("Error uploading attachments for proposalId={}", proposal.getId(), ex);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload attachments");
            }

            Proposal saved = proposalRepository.save(proposal);

            // Increment proposal count on job
            job.setProposalCount(job.getProposalCount() + 1);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);

            log.info("Proposal saved successfully proposalId={}, jobId={}, freelancerId={}", saved.getId(), jobId, freelancerId);
            return toResponse(saved);
        } catch (ResponseStatusException ex) {
            log.warn("Proposal submission rejected jobId={}, freelancerId={}, status={}, reason={}", jobId, freelancerId, ex.getStatusCode().value(), ex.getReason());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while submitting proposal jobId={}, freelancerId={}", jobId, freelancerId, ex);
            throw ex;
        }
    }

    public ProposalFileStorageService.StoredFile downloadStoredAttachment(String proposalId, String attachmentId, String requesterId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found"));

        Job job = jobRepository.findById(proposal.getJobId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        boolean canView = requesterId.equals(proposal.getFreelancerId()) || requesterId.equals(job.getEmployerId());
        if (!canView) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this attachment");
        }

        ProposalAttachment attachment = proposal.getAttachments().stream()
                .filter(item -> attachmentId.equals(item.getAttachmentId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));

        return proposalFileStorageService.download(attachment.getStorageKey(), attachment.getFileName(), attachment.getMimeType());
    }

    /**
     * Get proposals for a job (employer only)
     */
    public Page<ProposalDtos.ProposalResponse> getJobProposals(String jobId, String employerId, ProposalStatus status, Pageable pageable) {
        // Verify job owner
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        if (!job.getEmployerId().equals(employerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the job owner");
        }

        Page<Proposal> proposals;
        if (status != null) {
            proposals = proposalRepository.findByJobIdAndStatus(jobId, status, pageable);
        } else {
            proposals = proposalRepository.findByJobId(jobId, pageable);
        }

        return proposals.map(this::toResponse);
    }

    /**
     * Get proposals by freelancer
     */
    public Page<ProposalDtos.ProposalResponse> getFreelancerProposals(String freelancerId, ProposalStatus status, Pageable pageable) {
        Page<Proposal> proposals;
        if (status != null) {
            proposals = proposalRepository.findByFreelancerIdAndStatus(freelancerId, status, pageable);
        } else {
            proposals = proposalRepository.findByFreelancerId(freelancerId, pageable);
        }

        return proposals.map(this::toResponse);
    }

    /**
     * Shortlist proposal (employer only)
     */
    public ProposalDtos.ProposalResponse shortlistProposal(String proposalId, String employerId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found"));

        // Verify job owner
        Job job = jobRepository.findById(proposal.getJobId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        if (!job.getEmployerId().equals(employerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the job owner");
        }

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proposal is not pending");
        }

        // Shortlist this proposal; offer will be created in the next step
        proposal.setStatus(ProposalStatus.SHORTLISTED);
        proposal.setUpdatedAt(Instant.now());
        proposal.setRespondedAt(Instant.now());

        Proposal saved = proposalRepository.save(proposal);

        return toResponse(saved);
    }

    /**
     * Reject proposal (employer only)
     */
    public ProposalDtos.ProposalResponse rejectProposal(String proposalId, String employerId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found"));

        // Verify job owner
        Job job = jobRepository.findById(proposal.getJobId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        if (!job.getEmployerId().equals(employerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the job owner");
        }

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proposal is not pending");
        }

        proposal.setStatus(ProposalStatus.REJECTED);
        proposal.setUpdatedAt(Instant.now());
        proposal.setRespondedAt(Instant.now());
        Proposal saved = proposalRepository.save(proposal);

        return toResponse(saved);
    }

    /**
     * Withdraw proposal (freelancer only)
     */
    public ProposalDtos.ProposalResponse withdrawProposal(String proposalId, String freelancerId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found"));

        if (!proposal.getFreelancerId().equals(freelancerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the proposal author");
        }

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot withdraw non-pending proposal");
        }

        proposal.setStatus(ProposalStatus.WITHDRAWN);
        proposal.setUpdatedAt(Instant.now());
        Proposal saved = proposalRepository.save(proposal);

        Job job = jobRepository.findById(proposal.getJobId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        if (job.getProposalCount() > 0) {
            job.setProposalCount(job.getProposalCount() - 1);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
        }

        return toResponse(saved);
    }

    private ProposalDtos.ProposalResponse toResponse(Proposal proposal) {
        return new ProposalDtos.ProposalResponse(
                proposal.getId(),
                proposal.getJobId(),
                proposal.getJobTitle(),
                proposal.getFreelancerId(),
                proposal.getCoverLetter(),
                proposal.getEstimatedDuration(),
                proposal.getStatus(),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt(),
                proposal.getRespondedAt(),
                proposal.getAttachments() == null ? List.of() : proposal.getAttachments().stream().map(item -> toAttachmentResponse(proposal.getId(), item)).toList()
        );
    }

    private List<ProposalAttachment> uploadAttachments(String proposalId, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<ProposalAttachment> attachments = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            validateAttachment(file);

            String originalName = file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename();
            String safeFileName = StringUtils.cleanPath(originalName).replaceAll("[\\\\/]+", "_");
            String storageKey = "proposals/%s/%s-%s".formatted(proposalId, UUID.randomUUID(), safeFileName);

            proposalFileStorageService.upload(storageKey, file, proposalId);

            ProposalAttachment attachment = new ProposalAttachment();
            attachment.setAttachmentId(UUID.randomUUID().toString());
            attachment.setProposalId(proposalId);
            attachment.setFileName(safeFileName);
            attachment.setMimeType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setStorageKey(storageKey);
            attachment.setUploadedAt(Instant.now());
            attachments.add(attachment);
        }

        return attachments;
    }

    private void validateAttachment(MultipartFile file) {
        if (file.getSize() > MAX_ATTACHMENT_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each attachment must be 20MB or smaller");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported attachment type");
        }
    }

    private ProposalDtos.ProposalAttachmentResponse toAttachmentResponse(String proposalId, ProposalAttachment attachment) {
        return new ProposalDtos.ProposalAttachmentResponse(
                attachment.getAttachmentId(),
                attachment.getFileName(),
                attachment.getMimeType(),
                attachment.getFileSize(),
                attachment.getUploadedAt(),
                "/proposals/%s/attachments/%s/download".formatted(proposalId, attachment.getAttachmentId())
        );
    }
}
