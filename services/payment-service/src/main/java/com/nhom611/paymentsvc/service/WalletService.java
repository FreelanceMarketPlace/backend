package com.nhom611.paymentsvc.service;

import com.nhom611.paymentsvc.domain.Wallet;
import com.nhom611.paymentsvc.dto.PaymentDtos;
import com.nhom611.paymentsvc.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {
    
    private final WalletRepository walletRepository;

    /**
     * Lấy hoặc tạo wallet cho user
     */
    public Wallet getOrCreateWallet(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Wallet newWallet = Wallet.builder()
                            .walletId(UUID.randomUUID().toString())
                            .userId(userId)
                            .balance(0.0)
                            .frozenBalance(0.0)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return walletRepository.save(newWallet);
                });
    }

    /**
     * Lấy thông tin ví
     */
    public PaymentDtos.WalletResponse getWallet(String userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return mapWalletToResponse(wallet);
    }

    /**
     * Cộng tiền vào balance (khi nạp tiền thành công)
     */
    public Wallet addBalance(String userId, Double amount) {
        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        return walletRepository.save(wallet);
    }

    /**
     * Trừ tiền từ balance (khi fund escrow)
     */
    public Wallet subtractBalance(String userId, Double amount) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance() < amount) {
            throw new IllegalArgumentException("Số dư ví không đủ");
        }
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        return walletRepository.save(wallet);
    }

    /**
     * Khoá tiền (chuyển từ balance sang frozenBalance)
     */
    public Wallet lockBalance(String userId, Double amount) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance() < amount) {
            throw new IllegalArgumentException("Số dư ví không đủ để khoá");
        }
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setFrozenBalance(wallet.getFrozenBalance() + amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        return walletRepository.save(wallet);
    }

    /**
     * Mở khoá tiền (hoàn trả)
     */
    public Wallet unlockBalance(String userId, Double amount) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getFrozenBalance() < amount) {
            throw new IllegalArgumentException("Số dư bị khoá không đủ");
        }
        wallet.setFrozenBalance(wallet.getFrozenBalance() - amount);
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        return walletRepository.save(wallet);
    }

    /**
     * Giải ngân tiền (từ frozenBalance sang ví khác - freelancer)
     */
    public void releaseBalance(String employerUserId, String freelancerUserId, Double totalAmount) {
        // Deduct từ frozen balance của employer
        Wallet employerWallet = getOrCreateWallet(employerUserId);
        if (employerWallet.getFrozenBalance() < totalAmount) {
            throw new IllegalArgumentException("Số dư bị khoá không đủ");
        }
        employerWallet.setFrozenBalance(employerWallet.getFrozenBalance() - totalAmount);
        employerWallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(employerWallet);
        
        // Add 90% vào ví freelancer
        Double freelancerAmount = totalAmount * 0.9;
        addBalance(freelancerUserId, freelancerAmount);
    }

    private PaymentDtos.WalletResponse mapWalletToResponse(Wallet wallet) {
        return PaymentDtos.WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .frozenBalance(wallet.getFrozenBalance())
                .totalBalance(wallet.getBalance() + wallet.getFrozenBalance())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
