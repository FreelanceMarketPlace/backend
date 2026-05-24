package com.nhom611.contractsvc.domain;

public enum ContractStatus {
    ACTIVE,       // Hợp đồng đang hiệu lực / job ở trạng thái In Progress
    COMPLETED,    // Toàn bộ milestone đã hoàn tất
    CANCELLED     // Hợp đồng bị huỷ
}
