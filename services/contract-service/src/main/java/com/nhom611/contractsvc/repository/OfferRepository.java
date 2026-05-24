package com.nhom611.contractsvc.repository;

import com.nhom611.contractsvc.domain.Offer;
import com.nhom611.contractsvc.domain.OfferStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepository extends MongoRepository<Offer, String> {
    Optional<Offer> findByProposalId(String proposalId);
    List<Offer> findByJobId(String jobId);
    List<Offer> findByFreelancerId(String freelancerId);
    boolean existsByProposalId(String proposalId);
    List<Offer> findByStatus(OfferStatus status);
}
