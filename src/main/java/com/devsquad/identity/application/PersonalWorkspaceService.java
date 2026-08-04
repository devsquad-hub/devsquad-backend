package com.devsquad.identity.application;

import com.devsquad.identity.application.port.PersonalWorkspaceStore;
import com.devsquad.project.application.port.ProjectStore;
import com.devsquad.shared.security.AuthorizationService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PersonalWorkspaceService {
    private final PersonalWorkspaceStore workspace;
    private final AuthorizationService authorization;

    public PersonalWorkspaceService(PersonalWorkspaceStore workspace, AuthorizationService authorization) {
        this.workspace = workspace;
        this.authorization = authorization;
    }

    public List<ProjectStore.ProjectView> projects(String clerkUserId) {
        return workspace.projects(authorization.requireAccount(clerkUserId));
    }

    public List<PersonalWorkspaceStore.ApplicationSummary> applications(String clerkUserId) {
        return workspace.applications(authorization.requireAccount(clerkUserId));
    }

    public List<PersonalWorkspaceStore.InvitationSummary> invitations(String clerkUserId) {
        return workspace.invitations(authorization.requireAccount(clerkUserId));
    }
}
