package com.devsquad.shared.security;

import com.devsquad.hub.domain.HubRole;
import com.devsquad.project.domain.ProjectRole;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcIdentityAccessAdapter implements AccountIdentity, HubRoleReader, ProjectRoleReader {

    private final JdbcClient jdbc;

    public JdbcIdentityAccessAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> findActiveAccountId(String clerkUserId) {
        return jdbc.sql("select id from accounts where clerk_user_id = :clerkId and status = 'ACTIVE'")
                .param("clerkId", clerkUserId)
                .query(UUID.class)
                .optional();
    }

    @Override
    public Optional<HubRole> findActiveRole(UUID hubId, UUID accountId) {
        return jdbc.sql("""
                        select role from hub_memberships
                        where hub_id = :hubId and account_id = :accountId and status = 'ACTIVE'
                        """)
                .param("hubId", hubId)
                .param("accountId", accountId)
                .query(String.class)
                .optional()
                .map(HubRole::valueOf);
    }

    @Override
    public Optional<ProjectRole> findActiveProjectRole(UUID projectId, UUID accountId) {
        return jdbc.sql("""
                        select role from project_memberships
                        where project_id = :projectId and account_id = :accountId and status = 'ACTIVE'
                        """)
                .param("projectId", projectId).param("accountId", accountId)
                .query(String.class).optional().map(ProjectRole::valueOf);
    }
}
