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
@Document(collection = "deliverables")
public class Deliverable {
    @Id
    private String deliverableId;
    
    private String milestoneId;
    
    private String fileUrl;
    private String linkUrl;
    private String description;
    
    private LocalDateTime submittedAt;
    
    private LocalDateTime createdAt;
    
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
