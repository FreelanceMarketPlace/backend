package com.nhom611.contractsvc.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom611.common.events.OfferAcceptedEvent;
import com.nhom611.contractsvc.service.ContractService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaListeners {

    private final ContractService contractService;
    private final ObjectMapper objectMapper;

    public KafkaListeners(ContractService contractService, ObjectMapper objectMapper) {
        this.contractService = contractService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "offer.accepted", groupId = "contract-service")
    public void onOfferAccepted(String message) {
        try {
            OfferAcceptedEvent evt = objectMapper.readValue(message, OfferAcceptedEvent.class);
            if (evt != null && evt.getOfferId() != null) {
                // Create contract idempotently
                contractService.createContractFromOffer(evt.getOfferId());
            }
        } catch (Exception ex) {
            System.err.println("Failed to process offer.accepted: " + ex.getMessage());
        }
    }
}
