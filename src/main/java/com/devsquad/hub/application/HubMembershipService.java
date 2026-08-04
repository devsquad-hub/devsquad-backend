package com.devsquad.hub.application;

import com.devsquad.hub.application.port.HubMembershipStore;
import com.devsquad.hub.domain.HubRole;
import com.devsquad.shared.domain.DomainException;
import com.devsquad.shared.security.AuthorizationService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HubMembershipService {

    private final AuthorizationService authorization;
    private final HubMembershipStore memberships;

    public HubMembershipService(AuthorizationService authorization, HubMembershipStore memberships) {
        this.authorization = authorization;
        this.memberships = memberships;
    }

    public List<HubMembershipStore.MembershipView> mine(String clerkUserId) {
        return memberships.findByAccount(authorization.requireAccount(clerkUserId));
    }

    public List<HubMembershipStore.MemberView> members(String clerkUserId, UUID hubId) {
        authorization.requireHubMember(clerkUserId, hubId);
        var showEmail = authorization.canManageHub(clerkUserId, hubId);
        return memberships.findMembers(hubId).stream()
                .map(member -> new HubMembershipStore.MemberView(
                        member.accountId(), member.displayName(), showEmail ? member.email() : null,
                        member.avatarUrl(), member.role()))
                .toList();
    }

    @Transactional
    public void assign(String clerkUserId, UUID hubId, UUID accountId, HubRole role) {
        var actorId = authorization.requireHubMaster(clerkUserId, hubId);
        if (actorId.equals(accountId)) {
            throw new DomainException("master_self_demotion_forbidden", "The hub master cannot change their own role");
        }
        if (role == HubRole.MASTER) {
            throw new DomainException("master_transfer_not_supported", "Use the explicit master transfer operation");
        }
        if (!memberships.isActiveAccount(accountId)) {
            throw new DomainException("account_not_found", "Active account was not found");
        }
        memberships.assign(hubId, accountId, role);
    }
}
