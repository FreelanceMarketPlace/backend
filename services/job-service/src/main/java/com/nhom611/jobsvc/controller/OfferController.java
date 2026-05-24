package com.nhom611.jobsvc.controller;

import com.nhom611.jobsvc.domain.OfferStatus;
import com.nhom611.jobsvc.dto.OfferDtos;
import com.nhom611.jobsvc.service.OfferService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping("/proposals/{proposalId}/offers")
    public ResponseEntity<OfferDtos.OfferResponse> createOffer(
            @PathVariable String proposalId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OfferDtos.CreateOfferRequest req
    ) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(offerService.createOfferFromProposal(proposalId, jwt.getSubject(), req));
    }

    @GetMapping("/freelancer/offers")
    public ResponseEntity<Map<String, Object>> getReceivedOffers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) OfferStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 100));
        Page<OfferDtos.OfferResponse> offers = offerService.getReceivedOffers(jwt.getSubject(), status, pageable);
        return ResponseEntity.ok(Map.of(
                "items", offers.getContent(),
                "page", offers.getNumber(),
                "size", offers.getSize(),
                "totalElements", offers.getTotalElements(),
                "totalPages", offers.getTotalPages()
        ));
    }

    @PostMapping("/offers/{offerId}/accept")
    public ResponseEntity<OfferDtos.OfferResponse> acceptOffer(
            @PathVariable String offerId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        return ResponseEntity.ok(offerService.acceptOffer(offerId, jwt.getSubject()));
    }

    @PostMapping("/offers/{offerId}/decline")
    public ResponseEntity<OfferDtos.OfferResponse> declineOffer(
            @PathVariable String offerId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        return ResponseEntity.ok(offerService.declineOffer(offerId, jwt.getSubject()));
    }

    @GetMapping("/offers/{offerId}")
    public ResponseEntity<OfferDtos.OfferResponse> getOfferById(@PathVariable String offerId) {
        return ResponseEntity.ok(offerService.getOfferById(offerId));
    }
}