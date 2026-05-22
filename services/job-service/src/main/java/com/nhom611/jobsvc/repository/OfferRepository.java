package com.nhom611.jobsvc.repository;

import com.nhom611.jobsvc.domain.Offer;
import com.nhom611.jobsvc.domain.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OfferRepository extends MongoRepository<Offer, String> {

    boolean existsByProposalId(String proposalId);

    Optional<Offer> findByProposalId(String proposalId);

    Page<Offer> findByFreelancerId(String freelancerId, Pageable pageable);

    Page<Offer> findByFreelancerIdAndStatus(String freelancerId, OfferStatus status, Pageable pageable);

    Page<Offer> findByJobId(String jobId, Pageable pageable);

    Page<Offer> findByJobIdAndStatus(String jobId, OfferStatus status, Pageable pageable);

    List<Offer> findByJobIdAndStatus(String jobId, OfferStatus status);
}