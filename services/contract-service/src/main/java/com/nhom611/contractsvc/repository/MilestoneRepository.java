package com.nhom611.contractsvc.repository;

import com.nhom611.contractsvc.domain.Milestone;
import com.nhom611.contractsvc.domain.MilestoneStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MilestoneRepository extends MongoRepository<Milestone, String> {
    List<Milestone> findByContractId(String contractId);
    List<Milestone> findByContractIdAndStatus(String contractId, MilestoneStatus status);
    long countByContractIdAndStatus(String contractId, MilestoneStatus status);
}
