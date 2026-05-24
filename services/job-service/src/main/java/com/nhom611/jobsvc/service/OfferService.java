package com.nhom611.jobsvc.service;

import com.nhom611.jobsvc.domain.Job;
import com.nhom611.jobsvc.domain.JobStatus;
import com.nhom611.jobsvc.domain.Offer;
import com.nhom611.jobsvc.domain.OfferStatus;
import com.nhom611.jobsvc.domain.Proposal;
import com.nhom611.jobsvc.domain.ProposalStatus;
import com.nhom611.jobsvc.dto.OfferDtos;
import com.nhom611.jobsvc.repository.JobRepository;
import com.nhom611.jobsvc.repository.OfferRepository;
import com.nhom611.jobsvc.repository.ProposalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import com.nhom611.common.events.OfferAcceptedEvent;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final ProposalRepository proposalRepository;
    private final JobRepository jobRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OfferService(OfferRepository offerRepository, ProposalRepository proposalRepository, JobRepository jobRepository, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.offerRepository = offerRepository;
        this.proposalRepository = proposalRepository;
        this.jobRepository = jobRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public OfferDtos.OfferResponse createOfferFromProposal(String proposalId, String employerId, OfferDtos.CreateOfferRequest req) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found"));

        Job job = jobRepository.findById(proposal.getJobId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        if (!job.getEmployerId().equals(employerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the job owner");
        }

        if (proposal.getStatus() != ProposalStatus.SHORTLISTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proposal must be shortlisted before creating an offer");
        }

        if (job.getStatus() != JobStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job is no longer open for new offers");
        }

        if (offerRepository.existsByProposalId(proposalId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Offer already exists for this proposal");
        }

        BigDecimal contractValue;
        if (job.getBudgetType() == com.nhom611.jobsvc.domain.BudgetType.FIXED) {
            contractValue = job.getFixedBudget();
            if (contractValue == null || contractValue.signum() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job fixed budget is invalid");
            }
        } else {
            if (job.getHourlyRate() == null || job.getHourlyRate().signum() <= 0 || job.getEstimatedHours() == null || job.getEstimatedHours() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job hourly budget is invalid");
            }
            contractValue = job.getHourlyRate().multiply(BigDecimal.valueOf(job.getEstimatedHours()));
        }

        Instant now = Instant.now();
        Offer offer = new Offer();
        offer.setJobId(job.getId());
        offer.setProposalId(proposal.getId());
        offer.setEmployerId(job.getEmployerId());
        offer.setFreelancerId(proposal.getFreelancerId());
        offer.setJobDescription(req.jobDescription());
        offer.setContractValue(contractValue);
        offer.setEstimatedDuration(req.estimatedDuration());
        offer.setStatus(OfferStatus.PENDING);
        offer.setExpiresAt(req.expiresAt() != null ? req.expiresAt() : now.plusSeconds(7 * 24 * 60 * 60));
        offer.setCreatedAt(now);
        offer.setUpdatedAt(now);

        return toResponse(offerRepository.save(offer));
    }

    public Page<OfferDtos.OfferResponse> getReceivedOffers(String freelancerId, OfferStatus status, Pageable pageable) {
        Page<Offer> offers = status == null
                ? offerRepository.findByFreelancerId(freelancerId, pageable)
                : offerRepository.findByFreelancerIdAndStatus(freelancerId, status, pageable);
        return offers.map(this::toResponse);
    }

    public OfferDtos.OfferResponse acceptOffer(String offerId, String freelancerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));

        if (!offer.getFreelancerId().equals(freelancerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the offer recipient");
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offer is not pending");
        }

        Job job = jobRepository.findById(offer.getJobId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job has already been filled by another accepted offer");
        }

        Instant now = Instant.now();

        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setUpdatedAt(now);
        offer.setRespondedAt(now);

        job.setStatus(JobStatus.IN_PROGRESS);
        job.setUpdatedAt(now);
        jobRepository.save(job);

        List<Offer> siblingOffers = offerRepository.findByJobIdAndStatus(offer.getJobId(), OfferStatus.PENDING);
        for (Offer siblingOffer : siblingOffers) {
            if (!siblingOffer.getOfferId().equals(offer.getOfferId())) {
                siblingOffer.setStatus(OfferStatus.EXPIRED);
                siblingOffer.setUpdatedAt(now);
                siblingOffer.setRespondedAt(now);
            }
        }
        offerRepository.saveAll(siblingOffers);

        Offer saved = offerRepository.save(offer);

        // Publish OfferAccepted event to Kafka
        try {
            OfferAcceptedEvent evt = new OfferAcceptedEvent(
                    saved.getOfferId(),
                    saved.getJobId(),
                    saved.getProposalId(),
                    saved.getEmployerId(),
                    saved.getFreelancerId(),
                    saved.getContractValue() != null ? saved.getContractValue().doubleValue() : 0.0,
                    saved.getRespondedAt()
            );
            String payload = objectMapper.writeValueAsString(evt);
            kafkaTemplate.send("offer.accepted", saved.getOfferId(), payload);
        } catch (Exception ex) {
            // log and continue
            System.err.println("Failed to publish offer.accepted event: " + ex.getMessage());
        }

        return toResponse(saved);
    }

    public OfferDtos.OfferResponse declineOffer(String offerId, String freelancerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));

        if (!offer.getFreelancerId().equals(freelancerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the offer recipient");
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offer is not pending");
        }

        offer.setStatus(OfferStatus.DECLINED);
        offer.setUpdatedAt(Instant.now());
        offer.setRespondedAt(Instant.now());
        return toResponse(offerRepository.save(offer));
    }

    public OfferDtos.OfferResponse getOfferById(String offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
        return toResponse(offer);
    }

    private OfferDtos.OfferResponse toResponse(Offer offer) {
        return new OfferDtos.OfferResponse(
                offer.getOfferId(),
                offer.getJobId(),
                offer.getProposalId(),
                offer.getEmployerId(),
                offer.getFreelancerId(),
                offer.getJobDescription(),
                offer.getContractValue(),
                offer.getEstimatedDuration(),
                offer.getStatus(),
                offer.getExpiresAt(),
                offer.getCreatedAt(),
                offer.getUpdatedAt(),
                offer.getRespondedAt()
        );
    }
}