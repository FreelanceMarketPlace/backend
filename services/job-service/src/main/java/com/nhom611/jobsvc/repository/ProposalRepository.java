package com.nhom611.jobsvc.repository;

import com.nhom611.jobsvc.domain.Proposal;
import com.nhom611.jobsvc.domain.ProposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProposalRepository extends MongoRepository<Proposal, String> {
    /**
     * Check if freelancer already proposed for this job
     */
    boolean existsByJobIdAndFreelancerId(String jobId, String freelancerId);

    /**
     * Get a single proposal by jobId + freelancerId (should be unique)
     */
    Optional<Proposal> findByJobIdAndFreelancerId(String jobId, String freelancerId);

    /**
     * Get all proposals for a specific job
     */
    Page<Proposal> findByJobId(String jobId, Pageable pageable);

    /**
     * Get proposals for a job with specific status
     */
    Page<Proposal> findByJobIdAndStatus(String jobId, ProposalStatus status, Pageable pageable);

    /**
     * Get all proposals by a freelancer
     */
    Page<Proposal> findByFreelancerId(String freelancerId, Pageable pageable);

    /**
     * Get proposals by freelancer with specific status
     */
    Page<Proposal> findByFreelancerIdAndStatus(String freelancerId, ProposalStatus status, Pageable pageable);

    /**
     * Count accepted proposals for a job (to limit to 1 active contract)
     */
    long countByJobIdAndStatus(String jobId, ProposalStatus status);
}
