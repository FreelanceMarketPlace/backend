package com.nhom611.paymentsvc.dto;

import com.nhom611.paymentsvc.domain.DepositStatus;
import com.nhom611.paymentsvc.domain.TransactionStatus;
import com.nhom611.paymentsvc.domain.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class PaymentDtos {

    // ============ REQUEST DTOs ============

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateDepositRequest {
        private Double amount;  // Số tiền nạp
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfirmDepositRequest {
        private String transactionRefCode;  // Reference code từ QR quét
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LockWalletRequest {
        private Double amount;
        private String milestoneId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnlockWalletRequest {
        private Double amount;
        private String milestoneId;
    }

    // ============ RESPONSE DTOs ============

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WalletResponse {
        private String walletId;
        private String userId;
        private Double balance;
        private Double frozenBalance;
        private Double totalBalance;  // balance + frozenBalance
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DepositResponse {
        private String depositId;
        private String userId;
        private Double amount;
        private DepositStatus status;
        
        // QR code info
        private String qrCodeUrl;      // Data URL (base64 encoded image)
        private String qrCodeData;     // Mock account info
        
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionResponse {
        private String transactionId;
        private String userId;
        
        private TransactionType type;
        private TransactionStatus status;
        private Double amount;
        
        private String description;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionHistoryResponse {
        private List<TransactionResponse> transactions;
        private Long total;
        private Integer page;
        private Integer pageSize;
    }
}
