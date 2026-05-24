package com.nhom611.common.events;

import java.time.Instant;

public class OfferAcceptedEvent {
    private String offerId;
    private String jobId;
    private String proposalId;
    private String employerId;
    private String freelancerId;
    private Double contractValue;
    private Instant acceptedAt;

    public OfferAcceptedEvent() {}

    public OfferAcceptedEvent(String offerId, String jobId, String proposalId, String employerId, String freelancerId, Double contractValue, Instant acceptedAt) {
        this.offerId = offerId;
        this.jobId = jobId;
        this.proposalId = proposalId;
        this.employerId = employerId;
        this.freelancerId = freelancerId;
        this.contractValue = contractValue;
        this.acceptedAt = acceptedAt;
    }

    public String getOfferId() { return offerId; }
    public String getJobId() { return jobId; }
    public String getProposalId() { return proposalId; }
    public String getEmployerId() { return employerId; }
    public String getFreelancerId() { return freelancerId; }
    public Double getContractValue() { return contractValue; }
    public Instant getAcceptedAt() { return acceptedAt; }

    public void setOfferId(String offerId) { this.offerId = offerId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public void setProposalId(String proposalId) { this.proposalId = proposalId; }
    public void setEmployerId(String employerId) { this.employerId = employerId; }
    public void setFreelancerId(String freelancerId) { this.freelancerId = freelancerId; }
    public void setContractValue(Double contractValue) { this.contractValue = contractValue; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
}
