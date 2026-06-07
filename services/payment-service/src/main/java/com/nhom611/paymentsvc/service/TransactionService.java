package com.nhom611.paymentsvc.service;

import com.nhom611.paymentsvc.domain.Transaction;
import com.nhom611.paymentsvc.domain.TransactionStatus;
import com.nhom611.paymentsvc.domain.TransactionType;
import com.nhom611.paymentsvc.dto.PaymentDtos;
import com.nhom611.paymentsvc.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {
    
    private final TransactionRepository transactionRepository;

    /**
     * Tạo transaction log
     */
    public Transaction createTransaction(String userId, String walletId, 
                                        TransactionType type, TransactionStatus status,
                                        Double amount, String description) {
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .userId(userId)
                .walletId(walletId)
                .type(type)
                .status(status)
                .amount(amount)
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        return transactionRepository.save(transaction);
    }

    /**
     * Tạo transaction với liên kết milestone
     */
    public Transaction createTransactionWithMilestone(String userId, String walletId,
                                                      TransactionType type, TransactionStatus status,
                                                      Double amount, String milestoneId,
                                                      String contractId, String description) {
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .userId(userId)
                .walletId(walletId)
                .type(type)
                .status(status)
                .amount(amount)
                .relatedMilestoneId(milestoneId)
                .relatedContractId(contractId)
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        return transactionRepository.save(transaction);
    }

    /**
     * Lấy lịch sử giao dịch của user
     */
    public PaymentDtos.TransactionHistoryResponse getTransactionHistory(String userId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Transaction> transactionPage = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        List<PaymentDtos.TransactionResponse> responses = transactionPage.getContent()
                .stream()
                .map(this::mapTransactionToResponse)
                .collect(Collectors.toList());
        
        return PaymentDtos.TransactionHistoryResponse.builder()
                .transactions(responses)
                .total(transactionPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    /**
     * Lấy lịch sử theo loại giao dịch
     */
    public PaymentDtos.TransactionHistoryResponse getTransactionsByType(String userId, TransactionType type, 
                                                                         int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Transaction> transactionPage = transactionRepository
                .findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
        
        List<PaymentDtos.TransactionResponse> responses = transactionPage.getContent()
                .stream()
                .map(this::mapTransactionToResponse)
                .collect(Collectors.toList());
        
        return PaymentDtos.TransactionHistoryResponse.builder()
                .transactions(responses)
                .total(transactionPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    private PaymentDtos.TransactionResponse mapTransactionToResponse(Transaction transaction) {
        return PaymentDtos.TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .userId(transaction.getUserId())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
