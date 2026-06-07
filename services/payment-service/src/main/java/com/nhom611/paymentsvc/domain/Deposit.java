package com.nhom611.paymentsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Lưu thông tin deposit order (yêu cầu nạp tiền)
 * Mỗi order có 1 QR code duy nhất
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "deposits")
public class Deposit {
    @Id
    private String depositId;
    
    private String userId;           // FK → users (người nạp tiền)
    private String walletId;         // FK → wallets
    
    private Double amount;
    private DepositStatus status;
    
    // QR Code info (mock)
    private String qrCodeUrl;        // Data URL of QR code
    private String qrCodeData;       // QR content (mock bank account)
    
    private LocalDateTime expiresAt; // Hết hạn QR (15 phút)
    
    // Callback info
    private String transactionRefCode;  // Reference từ payment gateway
    
    private LocalDateTime completedAt;
    
    private LocalDateTime createdAt;
    
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
