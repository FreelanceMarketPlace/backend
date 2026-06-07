package com.nhom611.paymentsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String transactionId;
    
    private String walletId;         // FK → wallets
    private String userId;           // Denormalized for easier query
    
    private TransactionType type;
    private TransactionStatus status;
    
    private Double amount;
    
    // Optional fields for linking to other entities
    private String relatedMilestoneId;    // Nếu type = ESCROW_LOCK/MILESTONE_RELEASE
    private String relatedContractId;
    private String relatedEscrowId;
    private String relatedDepositId;
    
    private String description;
    
    private LocalDateTime createdAt;
    
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
