package com.nhom611.paymentsvc.repository;

import com.nhom611.paymentsvc.domain.Transaction;
import com.nhom611.paymentsvc.domain.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    Page<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, TransactionType type, Pageable pageable);
    
    @Query("{ 'userId': ?0, 'createdAt': { $gte: ?1, $lte: ?2 } }")
    List<Transaction> findByUserIdAndDateRange(String userId, LocalDateTime from, LocalDateTime to);
    
    List<Transaction> findByRelatedMilestoneId(String milestoneId);
}
