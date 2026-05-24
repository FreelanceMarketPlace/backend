package com.nhom611.contractsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "contracts")
public class Contract {
    @Id
    private String contractId;
    
    private String jobId;
    @Indexed(unique = true)
    private String offerId;
    
    private String employerId;
    private String freelancerId;
    
    private Double totalValue;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    private ContractStatus status;
    
    private LocalDateTime createdAt;
    
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
