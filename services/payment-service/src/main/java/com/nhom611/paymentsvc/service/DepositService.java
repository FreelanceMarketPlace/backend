package com.nhom611.paymentsvc.service;

import com.nhom611.paymentsvc.domain.Deposit;
import com.nhom611.paymentsvc.domain.DepositStatus;
import com.nhom611.paymentsvc.domain.TransactionStatus;
import com.nhom611.paymentsvc.domain.TransactionType;
import com.nhom611.paymentsvc.dto.PaymentDtos;
import com.nhom611.paymentsvc.repository.DepositRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service xử lý Deposit (nạp tiền)
 * Flow:
 * 1. User request nạp tiền → tạo Deposit order + QR code
 * 2. User quét QR (mock) → confirm deposit
 * 3. System update wallet + transaction log
 */
@Service
@RequiredArgsConstructor
public class DepositService {
    
    private final DepositRepository depositRepository;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final QRCodeService qrCodeService;

    /**
     * Bước 1: User request nạp tiền
     * Return QR code cho user quét
     */
    public PaymentDtos.DepositResponse initiateDeposit(String userId, Double amount) {
        // Validate
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
        }
        if (amount > 100_000_000) {  // Max 100M
            throw new IllegalArgumentException("Số tiền vượt quá giới hạn");
        }

        // Lấy hoặc tạo wallet
        var wallet = walletService.getOrCreateWallet(userId);

        // Tạo mock bank account info
        String mockBankAccount = generateMockBankAccount(userId);
        String mockAmount = String.format("%.2f", amount);
        String qrContent = String.format("BANK|%s|%s|%s", mockBankAccount, mockAmount, userId);

        // Generate QR code (base64 encoded)
        String qrCodeUrl = qrCodeService.generateQRCode(qrContent, 200);

        // Tạo Deposit order
        Deposit deposit = Deposit.builder()
                .depositId(UUID.randomUUID().toString())
                .userId(userId)
                .walletId(wallet.getWalletId())
                .amount(amount)
                .status(DepositStatus.PENDING)
                .qrCodeUrl(qrCodeUrl)
                .qrCodeData(qrContent)
                .expiresAt(LocalDateTime.now().plusMinutes(15))  // QR hết hạn sau 15 phút
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        depositRepository.save(deposit);

        // Create PENDING transaction log
        transactionService.createTransaction(
                userId, wallet.getWalletId(),
                TransactionType.DEPOSIT, TransactionStatus.PENDING,
                amount, "Nạp tiền vào ví (chờ xác nhận)"
        );

        return mapDepositToResponse(deposit);
    }

    /**
     * Bước 2: User quét QR (mock) → confirm deposit
     * Giả lập: User nhập ref code từ QR
     */
    public PaymentDtos.DepositResponse confirmDeposit(String userId, String depositId, String transactionRefCode) {
        // Find deposit
        Deposit deposit = depositRepository.findByDepositIdAndUserId(depositId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Deposit order không tìm thấy"));

        // Check status
        if (!deposit.getStatus().equals(DepositStatus.PENDING)) {
            throw new IllegalArgumentException("Chỉ có thể xác nhận deposit ở trạng thái PENDING");
        }

        // Check expiration
        if (LocalDateTime.now().isAfter(deposit.getExpiresAt())) {
            deposit.setStatus(DepositStatus.EXPIRED);
            depositRepository.save(deposit);
            throw new IllegalArgumentException("Mã QR đã hết hạn, vui lòng tạo mới");
        }

        // Mock: validate ref code (có thể check với payment gateway thực)
        if (!isValidRefCode(transactionRefCode)) {
            deposit.setStatus(DepositStatus.FAILED);
            depositRepository.save(deposit);
            
            // Update transaction to FAILED
            transactionService.createTransaction(
                    userId, deposit.getWalletId(),
                    TransactionType.DEPOSIT, TransactionStatus.FAILED,
                    deposit.getAmount(), "Nạp tiền thất bại"
            );
            
            throw new IllegalArgumentException("Reference code không hợp lệ");
        }

        // SUCCESS: Update wallet
        walletService.addBalance(userId, deposit.getAmount());

        // Update deposit
        deposit.setStatus(DepositStatus.COMPLETED);
        deposit.setTransactionRefCode(transactionRefCode);
        deposit.setCompletedAt(LocalDateTime.now());
        deposit.setUpdatedAt(LocalDateTime.now());
        depositRepository.save(deposit);

        // Update transaction to COMPLETED
        transactionService.createTransaction(
                userId, deposit.getWalletId(),
                TransactionType.DEPOSIT, TransactionStatus.COMPLETED,
                deposit.getAmount(), "Nạp tiền thành công"
        );

        return mapDepositToResponse(deposit);
    }

    /**
     * Lấy thông tin deposit
     */
    public PaymentDtos.DepositResponse getDeposit(String userId, String depositId) {
        Deposit deposit = depositRepository.findByDepositIdAndUserId(depositId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Deposit order không tìm thấy"));
        return mapDepositToResponse(deposit);
    }

    /**
     * Mock: Generate random bank account
     */
    private String generateMockBankAccount(String userId) {
        // Format: BANK_<userId_hash>_<random>
        return String.format("1234567890%s", userId.hashCode() % 100000);
    }

    /**
     * Mock: Validate ref code
     * Trong thực tế, gọi payment gateway API
     */
    private boolean isValidRefCode(String refCode) {
        // Mock: chấp nhận ref code có format "REF_" + digits
        return refCode != null && refCode.length() >= 5;
    }

    private PaymentDtos.DepositResponse mapDepositToResponse(Deposit deposit) {
        return PaymentDtos.DepositResponse.builder()
                .depositId(deposit.getDepositId())
                .userId(deposit.getUserId())
                .amount(deposit.getAmount())
                .status(deposit.getStatus())
                .qrCodeUrl(deposit.getQrCodeUrl())
                .qrCodeData(deposit.getQrCodeData())
                .expiresAt(deposit.getExpiresAt())
                .createdAt(deposit.getCreatedAt())
                .build();
    }
}
