package com.nhom611.contractsvc.repository;

import com.nhom611.contractsvc.domain.Contract;
import com.nhom611.contractsvc.domain.ContractStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends MongoRepository<Contract, String> {
    Optional<Contract> findByOfferId(String offerId);
    List<Contract> findByJobId(String jobId);
    List<Contract> findByEmployerId(String employerId);
    List<Contract> findByFreelancerId(String freelancerId);
    boolean existsByOfferId(String offerId);
    List<Contract> findByStatus(ContractStatus status);
}
