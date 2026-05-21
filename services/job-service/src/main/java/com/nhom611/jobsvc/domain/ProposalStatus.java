package com.nhom611.jobsvc.domain;

public enum ProposalStatus {
    // ACCEPTED kept for backward-compatibility with existing DB documents
    // (old flow used ACCEPTED on proposals). New flow uses SHORTLISTED + Offer.
    @Deprecated
    ACCEPTED,
    PENDING,
    SHORTLISTED,
    REJECTED,
    WITHDRAWN
}
