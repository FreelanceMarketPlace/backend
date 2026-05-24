package com.nhom611.contractsvc.repository;

import com.nhom611.contractsvc.domain.Escrow;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EscrowRepository extends MongoRepository<Escrow, String> {
    Optional<Escrow> findByMilestoneId(String milestoneId);
    boolean existsByMilestoneId(String milestoneId);
}
