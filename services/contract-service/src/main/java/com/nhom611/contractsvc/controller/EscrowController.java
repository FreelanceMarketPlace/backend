package com.nhom611.contractsvc.controller;

import com.nhom611.contractsvc.dto.ContractDtos;
import com.nhom611.contractsvc.service.EscrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/escrows")
@RequiredArgsConstructor
public class EscrowController {
    
    private final EscrowService escrowService;

    /**
     * Khoá tiền cho milestone (internal API)
     */
    @PostMapping("/{milestoneId}/lock")
    public ResponseEntity<ContractDtos.EscrowResponse> lockEscrow(
            @PathVariable String milestoneId,
            @RequestParam Double amount) {
        ContractDtos.EscrowResponse response = escrowService.lockEscrowForMilestone(milestoneId, amount);
        return ResponseEntity.ok(response);
    }

    /**
     * Giải ngân escrow (internal API)
     */
    @PostMapping("/{milestoneId}/release")
    public ResponseEntity<ContractDtos.EscrowResponse> releaseEscrow(
            @PathVariable String milestoneId) {
        ContractDtos.EscrowResponse response = escrowService.releaseEscrow(milestoneId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy escrow info
     */
    @GetMapping("/{milestoneId}")
    public ResponseEntity<ContractDtos.EscrowResponse> getEscrow(
            @PathVariable String milestoneId) {
        ContractDtos.EscrowResponse response = escrowService.getEscrowByMilestone(milestoneId);
        return ResponseEntity.ok(response);
    }
}
