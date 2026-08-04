package com.devsquad.hub.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HubAuthorizationPolicyTest {

    private final HubAuthorizationPolicy policy = new HubAuthorizationPolicy();

    @Test
    void onlyMasterCanManageHubAdministrators() {
        assertThat(policy.canManageHubAdministrators(HubRole.MASTER)).isTrue();
        assertThat(policy.canManageHubAdministrators(HubRole.ADMIN)).isFalse();
        assertThat(policy.canManageHubAdministrators(HubRole.MEMBER)).isFalse();
    }

    @Test
    void masterAndHubAdminCanReviewProjectProposals() {
        assertThat(policy.canReviewProjectProposals(HubRole.MASTER)).isTrue();
        assertThat(policy.canReviewProjectProposals(HubRole.ADMIN)).isTrue();
        assertThat(policy.canReviewProjectProposals(HubRole.MEMBER)).isFalse();
    }
}
