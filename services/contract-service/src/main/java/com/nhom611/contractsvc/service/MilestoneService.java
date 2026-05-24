package com.nhom611.contractsvc.service;

import com.nhom611.contractsvc.domain.*;
import com.nhom611.contractsvc.dto.ContractDtos;
import com.nhom611.contractsvc.repository.DeliverableRepository;
import com.nhom611.contractsvc.repository.MilestoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MilestoneService {
    
    private final MilestoneRepository milestoneRepository;
    private final DeliverableRepository deliverableRepository;

    /**
     * Freelancer submit deliverable cho milestone
     */
    public ContractDtos.DeliverableResponse submitMilestone(String milestoneId, 
                                                            ContractDtos.MilestoneSubmitRequest request) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found: " + milestoneId));

        // Kiểm tra milestone ở trạng thái IN_PROGRESS
        if (!MilestoneStatus.IN_PROGRESS.equals(milestone.getStatus())) {
            throw new RuntimeException("Milestone is not in IN_PROGRESS status");
        }

        // Tạo deliverable mới
        Deliverable deliverable = Deliverable.builder()
                .deliverableId(UUID.randomUUID().toString())
                .milestoneId(milestoneId)
                .fileUrl(request.getFileUrl())
                .linkUrl(request.getLinkUrl())
                .description(request.getDescription())
                .submittedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        Deliverable savedDeliverable = deliverableRepository.save(deliverable);

        // Cập nhật milestone thành SUBMITTED
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setUpdatedAt(LocalDateTime.now());
        milestoneRepository.save(milestone);

        return mapDeliverableToResponse(savedDeliverable);
    }

    /**
     * Employer yêu cầu chỉnh sửa milestone
     */
    public ContractDtos.MilestoneResponse requestRevision(String milestoneId, 
                                                          ContractDtos.RevisionRequest request) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found: " + milestoneId));

        // Kiểm tra milestone ở trạng thái SUBMITTED
        if (!MilestoneStatus.SUBMITTED.equals(milestone.getStatus())) {
            throw new RuntimeException("Milestone is not in SUBMITTED status");
        }

        // Kiểm tra revision count không vượt max
        if (milestone.getRevisionCount() >= milestone.getMaxRevisions()) {
            throw new RuntimeException("Maximum revisions reached");
        }

        // Tăng revision count
        milestone.setRevisionCount(milestone.getRevisionCount() + 1);
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        milestone.setUpdatedAt(LocalDateTime.now());

        Milestone updated = milestoneRepository.save(milestone);
        return mapMilestoneToResponse(updated);
    }

    /**
     * Employer approve milestone
     */
    public ContractDtos.MilestoneResponse approveMilestone(String milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found: " + milestoneId));

        // Kiểm tra milestone ở trạng thái SUBMITTED
        if (!MilestoneStatus.SUBMITTED.equals(milestone.getStatus())) {
            throw new RuntimeException("Milestone is not in SUBMITTED status");
        }

        milestone.setStatus(MilestoneStatus.APPROVED);
        milestone.setUpdatedAt(LocalDateTime.now());

        Milestone updated = milestoneRepository.save(milestone);
        return mapMilestoneToResponse(updated);
    }

    /**
     * Lấy danh sách milestone của contract
     */
    public List<ContractDtos.MilestoneResponse> getMilestonesByContract(String contractId) {
        List<Milestone> milestones = milestoneRepository.findByContractId(contractId);
        return milestones.stream()
                .map(this::mapMilestoneToResponse)
                .toList();
    }

    /**
     * Kiểm tra tất cả milestone đã approved chưa
     */
    public boolean areAllMilestonesApproved(String contractId) {
        List<Milestone> milestones = milestoneRepository.findByContractId(contractId);
        return milestones.stream()
                .allMatch(m -> MilestoneStatus.APPROVED.equals(m.getStatus()));
    }

    private ContractDtos.MilestoneResponse mapMilestoneToResponse(Milestone milestone) {
        return ContractDtos.MilestoneResponse.builder()
                .id(milestone.getMilestoneId())
                .contractId(milestone.getContractId())
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .amount(milestone.getAmount())
                .dueDate(milestone.getDueDate())
                .status(milestone.getStatus())
                .revisionCount(milestone.getRevisionCount())
                .maxRevisions(milestone.getMaxRevisions())
                .createdAt(milestone.getCreatedAt())
                .build();
    }

    private ContractDtos.DeliverableResponse mapDeliverableToResponse(Deliverable deliverable) {
        return ContractDtos.DeliverableResponse.builder()
                .id(deliverable.getDeliverableId())
                .milestoneId(deliverable.getMilestoneId())
                .fileUrl(deliverable.getFileUrl())
                .linkUrl(deliverable.getLinkUrl())
                .description(deliverable.getDescription())
                .submittedAt(deliverable.getSubmittedAt())
                .build();
    }
}
