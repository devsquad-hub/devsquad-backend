package com.devsquad.shared.security;

import com.devsquad.hub.domain.HubRole;
import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface HubRoleReader {
    Optional<HubRole> findActiveRole(UUID hubId, UUID accountId);
}
