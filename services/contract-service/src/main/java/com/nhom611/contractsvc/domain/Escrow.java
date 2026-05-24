package com.nhom611.contractsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "escrows")
public class Escrow {
    @Id
    private String escrowId;
    
    private String milestoneId;
    
    private Double amount;
    private Boolean isFrozen;
    
    private LocalDateTime lockedAt;
    private LocalDateTime releasedAt;
    
    private LocalDateTime createdAt;
    
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
