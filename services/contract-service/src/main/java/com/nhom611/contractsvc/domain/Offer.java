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
@Document(collection = "offers")
public class Offer {
    @Id
    private String offerId;
    
    private String jobId;
    private String proposalId;
    
    private String employerId;
    private String freelancerId;
    
    private String jobDescription;
    private Double contractValue;
    private String estimatedDuration;
    
    private OfferStatus status;
    
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
