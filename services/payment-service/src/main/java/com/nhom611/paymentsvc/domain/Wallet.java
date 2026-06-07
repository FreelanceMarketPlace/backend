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
@Document(collection = "wallets")
public class Wallet {
    @Id
    private String walletId;
    
    private String userId;           // FK → users
    
    private Double balance;          // Số dư khả dụng
    private Double frozenBalance;    // Số dư bị khoá trong escrow
    
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    
    @Builder.Default
    private LocalDateTime lastModified = LocalDateTime.now();
}
