package com.nhom611.paymentsvc.controller;

import com.nhom611.paymentsvc.dto.PaymentDtos;
import com.nhom611.paymentsvc.domain.TransactionType;
import com.nhom611.paymentsvc.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Transaction API
 * Endpoints để xem lịch sử giao dịch
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    
    private final TransactionService transactionService;

    /**
     * GET /api/transactions
     * Lấy lịch sử giao dịch của user
     * Query params: page=0, pageSize=10
     */
    @GetMapping
    public ResponseEntity<PaymentDtos.TransactionHistoryResponse> getTransactionHistory(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        PaymentDtos.TransactionHistoryResponse response = 
                transactionService.getTransactionHistory(userId, page, pageSize);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/transactions/filter
     * Lấy lịch sử giao dịch theo loại
     * Query params: type=DEPOSIT, page=0, pageSize=10
     */
    @GetMapping("/filter")
    public ResponseEntity<PaymentDtos.TransactionHistoryResponse> getTransactionsByType(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        PaymentDtos.TransactionHistoryResponse response = 
                transactionService.getTransactionsByType(userId, type, page, pageSize);
        return ResponseEntity.ok(response);
    }
}
