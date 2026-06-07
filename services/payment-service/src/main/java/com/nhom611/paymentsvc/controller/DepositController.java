package com.nhom611.paymentsvc.controller;

import com.nhom611.paymentsvc.dto.PaymentDtos;
import com.nhom611.paymentsvc.service.DepositService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Deposit API
 * Endpoints để nạp tiền vào wallet
 * 
 * Flow:
 * 1. POST /api/deposits/initiate → Khởi tạo deposit, return QR code
 * 2. POST /api/deposits/{depositId}/confirm → Xác nhận deposit (quét QR)
 */
@RestController
@RequestMapping("/api/deposits")
@RequiredArgsConstructor
public class DepositController {
    
    private final DepositService depositService;

    /**
     * POST /api/deposits/initiate
     * Bước 1: Khởi tạo deposit order + generate QR code
     * 
     * Request body: { "amount": 500000 }
     * Response: { "depositId", "qrCodeUrl", "qrCodeData", "expiresAt", ... }
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentDtos.DepositResponse> initiateDeposit(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody PaymentDtos.CreateDepositRequest request) {
        
        PaymentDtos.DepositResponse response = depositService.initiateDeposit(userId, request.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/deposits/{depositId}/confirm
     * Bước 2: Confirm deposit (user quét QR, nhập ref code)
     * 
     * Request body: { "transactionRefCode": "REF_12345" }
     * Response: { "depositId", "status": "COMPLETED", ... }
     */
    @PostMapping("/{depositId}/confirm")
    public ResponseEntity<PaymentDtos.DepositResponse> confirmDeposit(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String depositId,
            @RequestBody PaymentDtos.ConfirmDepositRequest request) {
        
        PaymentDtos.DepositResponse response = depositService.confirmDeposit(
                userId, depositId, request.getTransactionRefCode()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/deposits/{depositId}
     * Lấy thông tin deposit order
     */
    @GetMapping("/{depositId}")
    public ResponseEntity<PaymentDtos.DepositResponse> getDeposit(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String depositId) {
        
        PaymentDtos.DepositResponse response = depositService.getDeposit(userId, depositId);
        return ResponseEntity.ok(response);
    }
}
