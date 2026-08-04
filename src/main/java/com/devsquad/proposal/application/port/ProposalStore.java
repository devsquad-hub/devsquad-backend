package com.devsquad.proposal.application.port;

import com.devsquad.proposal.domain.ProjectProposal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProposalStore {
  ProposalView create(UUID id, UUID hubId, UUID authorId, ProposalContent content);

  Optional<ProjectProposal> find(UUID proposalId);

  ProposalView update(ProjectProposal proposal, ProposalContent content);

  ProposalView saveState(ProjectProposal proposal);

  ProposalView approveAndCreate(
      ProjectProposal proposal, ProposalContent content, String projectSlug, String projectKey);

  Optional<ProposalContent> findContent(UUID proposalId);

  List<ProposalView> findByHub(UUID hubId, UUID actorId, boolean canReview);

  record ProposalContent(
      String title,
      String summary,
      String problem,
      String proposedSolution,
      String goals,
      List<String> desiredSkills) {}

  record ProposalView(
      UUID id,
      UUID hubId,
      UUID authorId,
      String authorName,
      ProposalContent content,
      String status,
      UUID reviewerId,
      String decisionReason,
      UUID projectId,
      Instant createdAt,
      Instant updatedAt) {}
}
