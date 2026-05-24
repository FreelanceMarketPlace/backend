package com.nhom611.contractsvc.service;

import com.nhom611.contractsvc.domain.Escrow;
import com.nhom611.contractsvc.dto.ContractDtos;
import com.nhom611.contractsvc.repository.EscrowRepository;
import com.nhom611.contractsvc.repository.MilestoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EscrowService {
    
    private final EscrowRepository escrowRepository;
    private final MilestoneRepository milestoneRepository;

    /**
     * Khoá tiền cho milestone (frozen)
     */
    public ContractDtos.EscrowResponse lockEscrowForMilestone(String milestoneId, Double amount) {
        // Kiểm tra escrow chưa tồn tại
        if (escrowRepository.existsByMilestoneId(milestoneId)) {
            throw new RuntimeException("Escrow already exists for this milestone");
        }

        Escrow escrow = Escrow.builder()
                .escrowId(UUID.randomUUID().toString())
                .milestoneId(milestoneId)
                .amount(amount)
                .isFrozen(true)
                .lockedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        Escrow saved = escrowRepository.save(escrow);
        return mapEscrowToResponse(saved);
    }

    /**
     * Giải ngân escrow sau khi milestone được approve
     */
    public ContractDtos.EscrowResponse releaseEscrow(String milestoneId) {
        Escrow escrow = escrowRepository.findByMilestoneId(milestoneId)
                .orElseThrow(() -> new RuntimeException("Escrow not found for milestone: " + milestoneId));

        if (!escrow.getIsFrozen()) {
            throw new RuntimeException("Escrow is not frozen");
        }

        escrow.setIsFrozen(false);
        escrow.setReleasedAt(LocalDateTime.now());
        escrow.setUpdatedAt(LocalDateTime.now());

        Escrow released = escrowRepository.save(escrow);
        return mapEscrowToResponse(released);
    }

    /**
     * Lấy escrow theo milestone
     */
    public ContractDtos.EscrowResponse getEscrowByMilestone(String milestoneId) {
        Escrow escrow = escrowRepository.findByMilestoneId(milestoneId)
                .orElseThrow(() -> new RuntimeException("Escrow not found for milestone: " + milestoneId));
        return mapEscrowToResponse(escrow);
    }

    private ContractDtos.EscrowResponse mapEscrowToResponse(Escrow escrow) {
        return ContractDtos.EscrowResponse.builder()
                .id(escrow.getEscrowId())
                .milestoneId(escrow.getMilestoneId())
                .amount(escrow.getAmount())
                .isFrozen(escrow.getIsFrozen())
                .lockedAt(escrow.getLockedAt())
                .releasedAt(escrow.getReleasedAt())
                .build();
    }
}
