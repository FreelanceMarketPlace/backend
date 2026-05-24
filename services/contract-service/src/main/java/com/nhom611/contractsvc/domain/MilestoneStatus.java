package com.nhom611.contractsvc.domain;

public enum MilestoneStatus {
    NOT_STARTED,  // Chưa bắt đầu
    IN_PROGRESS,  // Đang thực hiện
    SUBMITTED,    // Freelancer đã nộp deliverable
    APPROVED,     // Employer đã duyệt và release escrow
    DISPUTED      // Có tranh chấp
}
