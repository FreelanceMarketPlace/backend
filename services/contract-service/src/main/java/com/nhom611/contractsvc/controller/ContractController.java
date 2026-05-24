package com.nhom611.contractsvc.controller;

import com.nhom611.contractsvc.dto.ContractDtos;
import com.nhom611.contractsvc.service.ContractService;
import com.nhom611.contractsvc.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {
    
    private final ContractService contractService;
    private final MilestoneService milestoneService;

    /**
     * Tạo contract từ offer (gọi khi freelancer accept offer)
     */
    @PostMapping("/from-offer/{offerId}")
    public ResponseEntity<ContractDtos.ContractResponse> createContractFromOffer(
            @PathVariable String offerId) {
        ContractDtos.ContractResponse response = contractService.createContractFromOffer(offerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy chi tiết contract
     */
    @GetMapping("/{contractId}")
    public ResponseEntity<ContractDtos.ContractResponse> getContractDetail(
            @PathVariable String contractId) {
        ContractDtos.ContractResponse response = contractService.getContractDetail(contractId);
        return ResponseEntity.ok(response);
    }

    /**
     * Employer xem danh sách contract của mình
     */
    @GetMapping("/employer/{employerId}")
    public ResponseEntity<List<ContractDtos.ContractResponse>> getEmployerContracts(
            @PathVariable String employerId) {
        return ResponseEntity.ok(contractService.getEmployerContracts(employerId));
    }

    /**
     * Freelancer xem danh sách contract của mình
     */
    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<ContractDtos.ContractResponse>> getFreelancerContracts(
            @PathVariable String freelancerId) {
        return ResponseEntity.ok(contractService.getFreelancerContracts(freelancerId));
    }

    /**
     * Lấy danh sách milestone của contract
     */
    @GetMapping("/{contractId}/milestones")
    public ResponseEntity<List<ContractDtos.MilestoneResponse>> getContractMilestones(
            @PathVariable String contractId) {
        List<ContractDtos.MilestoneResponse> milestones = milestoneService.getMilestonesByContract(contractId);
        return ResponseEntity.ok(milestones);
    }
}
