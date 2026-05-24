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
@Document(collection = "milestones")
public class Milestone {
    @Id
    private String milestoneId;
    
    private String contractId;
    
    private String title;
    private String description;
    private Double amount;
    private LocalDateTime dueDate;
    
    private MilestoneStatus status;
    
    private Integer revisionCount;
    private Integer maxRevisions;
    
    private LocalDateTime createdAt;
    
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
