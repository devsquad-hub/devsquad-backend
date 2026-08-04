package com.devsquad.proposal.application;

import com.devsquad.proposal.application.port.ProposalStore;
import com.devsquad.proposal.domain.ProjectProposal;
import com.devsquad.shared.domain.DomainException;
import com.devsquad.shared.security.AuthorizationService;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProposalService {

    private final AuthorizationService authorization;
    private final ProposalStore proposals;

    public ProposalService(AuthorizationService authorization, ProposalStore proposals) {
        this.authorization = authorization;
        this.proposals = proposals;
    }

    @Transactional
    public ProposalStore.ProposalView create(String clerkId, UUID hubId, ProposalStore.ProposalContent content) {
        var authorId = authorization.requireHubMember(clerkId, hubId);
        var proposal = ProjectProposal.draft(UUID.randomUUID(), hubId, authorId, content.title(), content.summary());
        return proposals.create(proposal.id(), hubId, authorId, normalize(content));
    }

    @Transactional
    public ProposalStore.ProposalView edit(String clerkId, UUID proposalId, ProposalStore.ProposalContent content) {
        var proposal = get(proposalId);
        requireAuthor(clerkId, proposal);
        proposal.edit(content.title(), content.summary());
        return proposals.update(proposal, normalize(content));
    }

    @Transactional
    public ProposalStore.ProposalView submit(String clerkId, UUID proposalId) {
        var proposal = get(proposalId);
        requireAuthor(clerkId, proposal);
        proposal.submit();
        return proposals.saveState(proposal);
    }

    public List<ProposalStore.ProposalView> list(String clerkId, UUID hubId) {
        var actorId = authorization.requireHubMember(clerkId, hubId);
        var canReview = false;
        try {
            authorization.requireHubManager(clerkId, hubId);
            canReview = true;
        } catch (DomainException ignored) {
            // A regular member may still list their own proposals.
        }
        return proposals.findByHub(hubId, actorId, canReview);
    }

    @Transactional
    public ProposalStore.ProposalView approve(String clerkId, UUID proposalId) {
        var proposal = get(proposalId);
        var reviewerId = authorization.requireHubManager(clerkId, proposal.hubId());
        var content = proposals.findContent(proposalId).orElseThrow();
        var projectId = UUID.randomUUID();
        proposal.approve(reviewerId, projectId);
        var suffix = projectId.toString().substring(0, 6);
        return proposals.approveAndCreate(proposal, content, slug(content.title()) + "-" + suffix, "PRJ" + suffix.toUpperCase(Locale.ROOT));
    }

    @Transactional
    public ProposalStore.ProposalView reject(String clerkId, UUID proposalId, String reason) {
        var proposal = get(proposalId);
        var reviewerId = authorization.requireHubManager(clerkId, proposal.hubId());
        proposal.reject(reviewerId, reason);
        return proposals.saveState(proposal);
    }

    private ProjectProposal get(UUID proposalId) {
        return proposals.find(proposalId)
                .orElseThrow(() -> new DomainException("proposal_not_found", "Proposal was not found"));
    }

    private void requireAuthor(String clerkId, ProjectProposal proposal) {
        if (!authorization.requireHubMember(clerkId, proposal.hubId()).equals(proposal.authorId())) {
            throw new DomainException("proposal_author_required", "Only the proposal author can perform this operation");
        }
    }

    private static ProposalStore.ProposalContent normalize(ProposalStore.ProposalContent content) {
        return new ProposalStore.ProposalContent(content.title().trim(), content.summary().trim(), trim(content.problem()),
                trim(content.proposedSolution()), trim(content.goals()), content.desiredSkills() == null ? List.of() : content.desiredSkills());
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String slug(String value) {
        var normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        var slug = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "project" : slug.substring(0, Math.min(slug.length(), 90));
    }
}
