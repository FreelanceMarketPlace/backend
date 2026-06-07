package com.nhom611.paymentsvc.controller;

import com.nhom611.paymentsvc.dto.PaymentDtos;
import com.nhom611.paymentsvc.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Wallet API
 * Endpoints để xem thông tin ví
 */
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {
    
    private final WalletService walletService;

    /**
     * GET /api/wallets/{userId}
     * Lấy thông tin ví của user
     */
    @GetMapping("/{userId}")
    public ResponseEntity<PaymentDtos.WalletResponse> getWallet(@PathVariable String userId) {
        PaymentDtos.WalletResponse response = walletService.getWallet(userId);
        return ResponseEntity.ok(response);
    }
}
