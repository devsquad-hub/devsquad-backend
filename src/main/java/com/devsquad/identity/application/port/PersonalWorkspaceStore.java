package com.devsquad.identity.application.port;

import com.devsquad.project.application.port.ProjectStore;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface PersonalWorkspaceStore {
    List<ProjectStore.ProjectView> projects(UUID accountId);
    List<ApplicationSummary> applications(UUID accountId);
    List<InvitationSummary> invitations(UUID accountId);

    record ApplicationSummary(
            UUID id, UUID projectId, String projectName, UUID positionId, String positionTitle,
            String status, OffsetDateTime submittedAt) {}

    record InvitationSummary(
            UUID id, UUID projectId, String projectName, UUID positionId, String functionalRole,
            String status, OffsetDateTime expiresAt) {}
}
