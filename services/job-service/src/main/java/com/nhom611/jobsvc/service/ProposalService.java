package com.nhom611.jobsvc.service;

import com.nhom611.jobsvc.domain.Job;
import com.nhom611.jobsvc.domain.JobStatus;
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
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class ProposalService {

    private static final Logger log = LoggerFactory.getLogger(ProposalService.class);

    private final ProposalRepository proposalRepository;
    private final JobRepository jobRepository;

    public ProposalService(ProposalRepository proposalRepository, JobRepository jobRepository) {
        this.proposalRepository = proposalRepository;
        this.jobRepository = jobRepository;
    }

    /**
     * Submit a proposal for a job
     */
    public ProposalDtos.ProposalResponse submitProposal(String jobId, String freelancerId, ProposalDtos.SubmitProposalRequest req) {
        log.info("Submitting proposal jobId={}, freelancerId={}, bidAmount={}", jobId, freelancerId, req.bidAmount());
        try {
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
            proposal.setJobId(jobId);
            proposal.setFreelancerId(freelancerId);
            proposal.setJobTitle(job.getTitle());
            proposal.setBidAmount(req.bidAmount());
            proposal.setMessage(req.message());
            proposal.setStatus(ProposalStatus.PENDING);
            proposal.setCreatedAt(Instant.now());
            proposal.setUpdatedAt(Instant.now());

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
     * Accept proposal (employer only)
     */
    public ProposalDtos.ProposalResponse acceptProposal(String proposalId, String employerId) {
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

        // Check if job already has an accepted proposal
        long acceptedCount = proposalRepository.countByJobIdAndStatus(proposal.getJobId(), ProposalStatus.ACCEPTED);
        if (acceptedCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This job already has an accepted proposal");
        }

        // Accept this proposal
        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setUpdatedAt(Instant.now());
        proposal.setRespondedAt(Instant.now());

        // Reject all other pending proposals for this job
        rejectOtherPendingProposals(proposal.getJobId(), proposalId);

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

        return toResponse(saved);
    }

    /**
     * Helper: reject all other pending proposals for a job
     */
    private void rejectOtherPendingProposals(String jobId, String excludeProposalId) {
        Page<Proposal> others = proposalRepository.findByJobIdAndStatus(jobId, ProposalStatus.PENDING, org.springframework.data.domain.Pageable.unpaged());
        others.getContent().stream()
                .filter(p -> !p.getId().equals(excludeProposalId))
                .forEach(p -> {
                    p.setStatus(ProposalStatus.REJECTED);
                    p.setUpdatedAt(Instant.now());
                    p.setRespondedAt(Instant.now());
                    proposalRepository.save(p);
                });
    }

    private ProposalDtos.ProposalResponse toResponse(Proposal proposal) {
        return new ProposalDtos.ProposalResponse(
                proposal.getId(),
                proposal.getJobId(),
                proposal.getJobTitle(),
                proposal.getFreelancerId(),
                proposal.getBidAmount(),
                proposal.getMessage(),
                proposal.getStatus(),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt(),
                proposal.getRespondedAt()
        );
    }
}
