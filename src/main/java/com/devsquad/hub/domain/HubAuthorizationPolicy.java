package com.devsquad.hub.domain;

public final class HubAuthorizationPolicy {

    public boolean canManageHubAdministrators(HubRole role) {
        return role == HubRole.MASTER;
    }

    public boolean canReviewProjectProposals(HubRole role) {
        return role == HubRole.MASTER || role == HubRole.ADMIN;
    }
}
