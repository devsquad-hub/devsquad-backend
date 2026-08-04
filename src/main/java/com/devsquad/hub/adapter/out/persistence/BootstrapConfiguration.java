package com.devsquad.hub.adapter.out.persistence;

import com.devsquad.shared.domain.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class BootstrapConfiguration implements ApplicationRunner {

    private final JdbcClient jdbc;
    private final String hubName;
    private final String hubSlug;
    private final String masterClerkUserId;

    public BootstrapConfiguration(
            JdbcClient jdbc,
            @Value("${app.bootstrap.hub-name:}") String hubName,
            @Value("${app.bootstrap.hub-slug:}") String hubSlug,
            @Value("${app.bootstrap.master-clerk-user-id:}") String masterClerkUserId) {
        this.jdbc = jdbc;
        this.hubName = hubName;
        this.hubSlug = hubSlug;
        this.masterClerkUserId = masterClerkUserId;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        requireConfiguration();
        jdbc.sql("""
                        insert into accounts (clerk_user_id, display_name)
                        values (:clerkId, 'DevSquad master')
                        on conflict (clerk_user_id) do nothing
                        """)
                .param("clerkId", masterClerkUserId)
                .update();
        jdbc.sql("""
                        insert into hubs (name, slug) values (:name, :slug)
                        on conflict (slug) do update set name = excluded.name, updated_at = now()
                        """)
                .param("name", hubName)
                .param("slug", hubSlug)
                .update();
        jdbc.sql("""
                        insert into hub_memberships (hub_id, account_id, role)
                        select h.id, a.id, 'MEMBER'
                        from hubs h cross join accounts a
                        where h.slug = :slug and a.status = 'ACTIVE'
                        on conflict (hub_id, account_id) do update set status = 'ACTIVE', updated_at = now()
                        """)
                .param("slug", hubSlug)
                .update();
        jdbc.sql("""
                        insert into hub_memberships (hub_id, account_id, role)
                        select h.id, a.id, 'MASTER'
                        from hubs h join accounts a on a.clerk_user_id = :clerkId
                        where h.slug = :slug
                        on conflict (hub_id, account_id) do update set role = 'MASTER', status = 'ACTIVE', updated_at = now()
                        """)
                .param("clerkId", masterClerkUserId)
                .param("slug", hubSlug)
                .update();
    }

    private void requireConfiguration() {
        if (hubName.isBlank() || hubSlug.isBlank() || masterClerkUserId.isBlank()) {
            throw new DomainException("invalid_bootstrap_configuration",
                    "BOOTSTRAP_HUB_NAME, BOOTSTRAP_HUB_SLUG and BOOTSTRAP_MASTER_CLERK_USER_ID are required");
        }
    }
}
