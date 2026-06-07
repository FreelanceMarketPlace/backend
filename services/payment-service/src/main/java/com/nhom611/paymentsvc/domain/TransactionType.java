package com.nhom611.paymentsvc.domain;

public enum TransactionType {
    DEPOSIT,              // Nạp tiền vào wallet
    ESCROW_LOCK,          // Khoá tiền cho milestone
    MILESTONE_RELEASE,    // Giải ngân milestone (90%)
    PLATFORM_FEE,         // Phí hệ thống (10%)
    WITHDRAWAL_REQUEST,   // Yêu cầu rút tiền
    REFUND,               // Hoàn tiền
    DISPUTE_RESOLUTION    // Phán quyết tranh chấp
}
