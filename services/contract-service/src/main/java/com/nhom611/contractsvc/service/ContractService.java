package com.nhom611.contractsvc.service;

import com.nhom611.contractsvc.domain.*;
import com.nhom611.contractsvc.dto.ContractDtos;
import com.nhom611.contractsvc.repository.ContractRepository;
import com.nhom611.contractsvc.repository.MilestoneRepository;
import com.nhom611.contractsvc.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.time.format.DateTimeParseException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {
    
    private final ContractRepository contractRepository;
    private final OfferRepository offerRepository;
    private final MilestoneRepository milestoneRepository;
    private final RestTemplate restTemplate;

    @Value("${job.service.url:http://localhost:8082}")
    private String jobServiceUrl;

    /**
     * Tạo contract khi freelancer accept offer
     */
    public ContractDtos.ContractResponse createContractFromOffer(String offerId) {
        // Lấy offer
        Offer offer = offerRepository.findById(offerId).orElse(null);
        if (offer == null) {
            // Try to fetch from job-service
            try {
                Map<?, ?> res = restTemplate.getForObject(jobServiceUrl + "/offers/" + offerId, Map.class);
                if (res != null) {
                    offer = Offer.builder()
                            .offerId(stringOr(res.get("offerId"), res.get("id"), res.get("_id")))
                            .proposalId(stringOr(res.get("proposalId")))
                            .jobId(stringOr(res.get("jobId")))
                            .employerId(stringOr(res.get("employerId")))
                            .freelancerId(stringOr(res.get("freelancerId")))
                            .jobDescription(stringOr(res.get("jobDescription"), res.get("description")))
                            .contractValue(doubleOr(res.get("contractValue"), res.get("value")))
                            .estimatedDuration(stringOr(res.get("estimatedDuration")))
                            .status(OfferStatus.valueOf(stringOr(res.get("status"), "PENDING")))
                            .createdAt(parseLocalDateTime(res.get("createdAt")))
                            .expiresAt(parseLocalDateTime(res.get("expiresAt")))
                            .build();
                    // Save locally for faster future access
                    offerRepository.save(offer);
                }
            } catch (Exception ex) {
                // ignore and throw below
            }
        }

        if (offer == null) throw new RuntimeException("Offer not found: " + offerId);

        // Offer được xử lý sau khi freelancer đã accept, nên trạng thái hợp lệ là PENDING hoặc ACCEPTED
        if (offer.getStatus() != OfferStatus.PENDING && offer.getStatus() != OfferStatus.ACCEPTED) {
            throw new RuntimeException("Offer is not in a valid state for contract creation: " + offer.getStatus());
        }

        if (LocalDateTime.now().isAfter(offer.getExpiresAt())) {
            throw new RuntimeException("Offer has expired");
        }

        // Kiểm tra contract chưa tồn tại cho offer này
        if (contractRepository.existsByOfferId(offerId)) {
            throw new RuntimeException("Contract already exists for this offer");
        }

        // Tạo contract mới
        Contract contract = Contract.builder()
                .contractId(UUID.randomUUID().toString())
                .jobId(offer.getJobId())
                .offerId(offerId)
                .employerId(offer.getEmployerId())
                .freelancerId(offer.getFreelancerId())
                .totalValue(offer.getContractValue())
                .startDate(LocalDateTime.now())
                .endDate(null)  // Sẽ set sau khi biết duration
                .status(ContractStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

            Contract savedContract;
            try {
                savedContract = contractRepository.save(contract);
            } catch (DuplicateKeyException ex) {
                return mapContractToResponse(contractRepository.findByOfferId(offerId)
                    .orElseThrow(() -> new RuntimeException("Contract already exists for offer: " + offerId)));
            }

        // Cập nhật offer status thành ACCEPTED
        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setUpdatedAt(LocalDateTime.now());
        offerRepository.save(offer);

        // Khởi tạo milestones (được gọi từ một service khác hoặc từ ngoài)
        initMilestones(savedContract.getContractId());

        return mapContractToResponse(savedContract);
    }

    /**
     * Khởi tạo milestone list cho contract
     */
    public void initMilestones(String contractId) {
        // Lấy contract
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found: " + contractId));

        // Tạo milestone mặc định (có thể tùy chỉnh theo job requirements)
        // Tạm thời tạo 1 milestone duy nhất
        Milestone milestone = Milestone.builder()
                .milestoneId(UUID.randomUUID().toString())
                .contractId(contractId)
                .title("Project Delivery")
                .description("Complete project deliverables")
                .amount(contract.getTotalValue())
                .dueDate(LocalDateTime.now().plusDays(30))  // 30 ngày default
                .status(MilestoneStatus.NOT_STARTED)
                .revisionCount(0)
                .maxRevisions(3)
                .createdAt(LocalDateTime.now())
                .build();

        milestoneRepository.save(milestone);
    }

    /**
     * Lấy chi tiết contract
     */
    public ContractDtos.ContractResponse getContractDetail(String contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found: " + contractId));
        return mapContractToResponse(contract);
    }

    /**
     * Lấy danh sách contract của freelancer
     */
    public List<ContractDtos.ContractResponse> getFreelancerContracts(String freelancerId) {
        return contractRepository.findByFreelancerId(freelancerId).stream()
                .map(this::mapContractToResponse)
                .toList();
    }

    /**
     * Lấy danh sách contract của employer
     */
    public List<ContractDtos.ContractResponse> getEmployerContracts(String employerId) {
        return contractRepository.findByEmployerId(employerId).stream()
                .map(this::mapContractToResponse)
                .toList();
    }

    /**
     * Update contract status
     */
    public void updateContractStatus(String contractId, ContractStatus newStatus) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found: " + contractId));
        contract.setStatus(newStatus);
        contract.setUpdatedAt(LocalDateTime.now());
        contractRepository.save(contract);
    }

    private ContractDtos.ContractResponse mapContractToResponse(Contract contract) {
        return ContractDtos.ContractResponse.builder()
                .id(contract.getContractId())
                .jobId(contract.getJobId())
                .offerId(contract.getOfferId())
                .employerId(contract.getEmployerId())
                .freelancerId(contract.getFreelancerId())
                .totalValue(contract.getTotalValue())
                .status(contract.getStatus())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .createdAt(contract.getCreatedAt())
                .build();
    }

    private static String stringOr(Object... vals) {
        for (Object v : vals) {
            if (v == null) continue;
            String s = String.valueOf(v);
            if (!s.isBlank()) return s;
        }
        return null;
    }

    private static Double doubleOr(Object... vals) {
        for (Object v : vals) {
            if (v == null) continue;
            try {
                return Double.valueOf(String.valueOf(v));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0.0;
    }

    private static java.time.LocalDateTime parseLocalDateTime(Object v) {
        if (v == null) return null;
        try {
            String s = String.valueOf(v);
            return java.time.LocalDateTime.parse(s);
        } catch (DateTimeParseException e) {
            try {
                // try parse as Instant
                java.time.Instant inst = java.time.Instant.parse(String.valueOf(v));
                return java.time.LocalDateTime.ofInstant(inst, java.time.ZoneId.systemDefault());
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
