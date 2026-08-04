package com.devsquad.shared.security;

import com.devsquad.hub.domain.HubRole;
import com.devsquad.project.domain.ProjectRole;
import com.devsquad.shared.domain.DomainException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final AccountIdentity identities;
    private final HubRoleReader hubRoles;
    private final ProjectRoleReader projectRoles;

    public AuthorizationService(AccountIdentity identities, HubRoleReader hubRoles, ProjectRoleReader projectRoles) {
        this.identities = identities;
        this.hubRoles = hubRoles;
        this.projectRoles = projectRoles;
    }

    public UUID requireAccount(String clerkUserId) {
        if (clerkUserId == null || clerkUserId.isBlank()) {
            throw new DomainException("authentication_required", "Authentication is required");
        }
        return identities.findActiveAccountId(clerkUserId)
                .orElseThrow(() -> new DomainException("account_not_synchronized", "Account has not been synchronized yet"));
    }

    public UUID requireHubMember(String clerkUserId, UUID hubId) {
        var accountId = requireAccount(clerkUserId);
        hubRoles.findActiveRole(hubId, accountId)
                .orElseThrow(() -> new DomainException("hub_membership_required", "Active hub membership is required"));
        return accountId;
    }

    public UUID requireHubManager(String clerkUserId, UUID hubId) {
        var accountId = requireAccount(clerkUserId);
        var role = hubRoles.findActiveRole(hubId, accountId)
                .orElseThrow(() -> new DomainException("hub_management_forbidden", "Hub management permission is required"));
        if (role != HubRole.MASTER && role != HubRole.ADMIN) {
            throw new DomainException("hub_management_forbidden", "Hub management permission is required");
        }
        return accountId;
    }

    public UUID requireHubMaster(String clerkUserId, UUID hubId) {
        var accountId = requireAccount(clerkUserId);
        if (hubRoles.findActiveRole(hubId, accountId).orElse(null) != HubRole.MASTER) {
            throw new DomainException("hub_master_required", "Hub master permission is required");
        }
        return accountId;
    }

    public UUID requireProjectMember(String clerkUserId, UUID projectId) {
        var accountId = requireAccount(clerkUserId);
        projectRoles.findActiveProjectRole(projectId, accountId)
                .orElseThrow(() -> new DomainException("project_membership_required", "Active project membership is required"));
        return accountId;
    }

    public UUID requireProjectAdmin(String clerkUserId, UUID projectId) {
        var accountId = requireAccount(clerkUserId);
        if (projectRoles.findActiveProjectRole(projectId, accountId).orElse(null) != com.devsquad.project.domain.ProjectRole.ADMIN) {
            throw new DomainException("project_admin_required", "Project administrator permission is required");
        }
        return accountId;
    }

    public ViewerCapabilities capabilities(String clerkUserId, UUID hubId, UUID projectId) {
        var accountId = requireAccount(clerkUserId);
        var hubRole = hubRoles.findActiveRole(hubId, accountId).orElse(null);
        var projectRole = projectRoles.findActiveProjectRole(projectId, accountId).orElse(null);
        var hubManager = hubRole == HubRole.MASTER || hubRole == HubRole.ADMIN;
        var projectAdmin = projectRole == ProjectRole.ADMIN;
        var projectMember = projectRole != null;
        return new ViewerCapabilities(
                hubManager,
                hubManager,
                projectAdmin,
                projectAdmin,
                projectMember,
                hubRole != null && !projectMember);
    }

    public boolean canManageHub(String clerkUserId, UUID hubId) {
        var accountId = requireAccount(clerkUserId);
        var role = hubRoles.findActiveRole(hubId, accountId).orElse(null);
        return role == HubRole.MASTER || role == HubRole.ADMIN;
    }
}
