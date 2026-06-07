package com.nhom611.paymentsvc.domain;

public enum DepositStatus {
    PENDING,      // Chờ quét QR / thanh toán
    COMPLETED,    // Thanh toán thành công
    FAILED,       // Thanh toán thất bại
    EXPIRED       // Hết hạn QR
}
