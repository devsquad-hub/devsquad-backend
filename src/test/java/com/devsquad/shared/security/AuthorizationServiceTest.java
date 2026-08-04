package com.devsquad.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devsquad.hub.domain.HubRole;
import com.devsquad.shared.domain.DomainException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTest {

    private final UUID accountId = UUID.randomUUID();
    private final UUID hubId = UUID.randomUUID();

    @Test
    void masterCanManageHub() {
        var service = new AuthorizationService(clerkId -> Optional.of(accountId),
                (ignoredHub, ignoredAccount) -> Optional.of(HubRole.MASTER),
                (ignoredProject, ignoredAccount) -> Optional.empty());

        assertThat(service.requireHubManager("user_1", hubId)).isEqualTo(accountId);
    }

    @Test
    void regularMemberCannotManageHub() {
        var service = new AuthorizationService(clerkId -> Optional.of(accountId),
                (ignoredHub, ignoredAccount) -> Optional.of(HubRole.MEMBER),
                (ignoredProject, ignoredAccount) -> Optional.empty());

        assertThatThrownBy(() -> service.requireHubManager("user_1", hubId))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo("hub_management_forbidden");
    }
}
