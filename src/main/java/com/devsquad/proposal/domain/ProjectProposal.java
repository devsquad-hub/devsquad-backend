package com.devsquad.proposal.domain;

import com.devsquad.shared.domain.DomainException;
import java.util.Objects;
import java.util.UUID;

public final class ProjectProposal {

    private final UUID id;
    private final UUID hubId;
    private final UUID authorId;
    private String title;
    private String summary;
    private ProposalStatus status;
    private UUID reviewerId;
    private String decisionReason;
    private UUID projectId;

    private ProjectProposal(UUID id, UUID hubId, UUID authorId, String title, String summary) {
        this.id = Objects.requireNonNull(id);
        this.hubId = Objects.requireNonNull(hubId);
        this.authorId = Objects.requireNonNull(authorId);
        this.title = requireText(title, "title");
        this.summary = requireText(summary, "summary");
        this.status = ProposalStatus.DRAFT;
    }

    public static ProjectProposal draft(UUID id, UUID hubId, UUID authorId, String title, String summary) {
        return new ProjectProposal(id, hubId, authorId, title, summary);
    }

    public static ProjectProposal restore(
            UUID id, UUID hubId, UUID authorId, String title, String summary, ProposalStatus status,
            UUID reviewerId, String decisionReason, UUID projectId) {
        var proposal = new ProjectProposal(id, hubId, authorId, title, summary);
        proposal.status = Objects.requireNonNull(status);
        proposal.reviewerId = reviewerId;
        proposal.decisionReason = decisionReason;
        proposal.projectId = projectId;
        return proposal;
    }

    public void edit(String title, String summary) {
        if (status != ProposalStatus.DRAFT && status != ProposalStatus.REJECTED) {
            throw new DomainException("proposal_not_editable", "An approved or pending proposal cannot be edited");
        }
        this.title = requireText(title, "title");
        this.summary = requireText(summary, "summary");
    }

    public void submit() {
        if (status != ProposalStatus.DRAFT && status != ProposalStatus.REJECTED) {
            throw new DomainException("proposal_not_submittable", "Proposal cannot be submitted from " + status);
        }
        status = ProposalStatus.PENDING;
        reviewerId = null;
        decisionReason = null;
    }

    public void approve(UUID reviewerId, UUID projectId) {
        requirePending();
        this.status = ProposalStatus.APPROVED;
        this.reviewerId = Objects.requireNonNull(reviewerId);
        this.projectId = Objects.requireNonNull(projectId);
        this.decisionReason = null;
    }

    public void reject(UUID reviewerId, String reason) {
        requirePending();
        this.status = ProposalStatus.REJECTED;
        this.reviewerId = Objects.requireNonNull(reviewerId);
        this.decisionReason = requireText(reason, "reason");
    }

    private void requirePending() {
        if (status != ProposalStatus.PENDING) {
            throw new DomainException("proposal_not_pending", "Proposal is not pending");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException("invalid_" + field, field + " must not be blank");
        }
        return value.trim();
    }

    public UUID id() { return id; }
    public UUID hubId() { return hubId; }
    public UUID authorId() { return authorId; }
    public String title() { return title; }
    public String summary() { return summary; }
    public ProposalStatus status() { return status; }
    public UUID reviewerId() { return reviewerId; }
    public String decisionReason() { return decisionReason; }
    public UUID projectId() { return projectId; }
}
