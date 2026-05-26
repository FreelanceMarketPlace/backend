package com.nhom611.jobsvc.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("proposals")
@CompoundIndexes({
        @CompoundIndex(name = "proposals_job_freelancer", def = "{'jobId': 1, 'freelancerId': 1}", unique = true),
        @CompoundIndex(name = "proposals_job_status_createdAt", def = "{'jobId': 1, 'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "proposals_freelancer_createdAt", def = "{'freelancerId': 1, 'createdAt': -1}")
})
public class Proposal {

    @Id
    private String id;

    @Indexed
    private String jobId;

    @Indexed
    private String freelancerId;

    private String jobTitle;

    private String coverLetter;

    private Integer estimatedDuration;

    @Indexed
    private ProposalStatus status;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant respondedAt; // when employer accepted/rejected

    private List<ProposalAttachment> attachments = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(String freelancerId) {
        this.freelancerId = freelancerId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public Integer getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(Integer estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public List<ProposalAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<ProposalAttachment> attachments) {
        this.attachments = attachments;
    }
}
