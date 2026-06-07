package com.nhom611.paymentsvc.repository;

import com.nhom611.paymentsvc.domain.Deposit;
import com.nhom611.paymentsvc.domain.DepositStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepositRepository extends MongoRepository<Deposit, String> {
    Optional<Deposit> findByDepositIdAndUserId(String depositId, String userId);
    
    List<Deposit> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<Deposit> findByStatusAndExpiresAtBefore(DepositStatus status, java.time.LocalDateTime expiration);
}
