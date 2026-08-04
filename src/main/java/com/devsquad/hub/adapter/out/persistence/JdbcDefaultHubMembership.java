package com.devsquad.hub.adapter.out.persistence;

import com.devsquad.identity.application.port.DefaultHubMembership;
import com.devsquad.shared.domain.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDefaultHubMembership implements DefaultHubMembership {

    private final JdbcClient jdbc;
    private final String hubSlug;

    public JdbcDefaultHubMembership(
            JdbcClient jdbc,
            @Value("${app.membership.default-hub-slug:devsquad}") String hubSlug) {
        this.jdbc = jdbc;
        this.hubSlug = hubSlug;
    }

    @Override
    public void ensureFor(String clerkUserId) {
        var updated = jdbc.sql("""
                        insert into hub_memberships (hub_id, account_id, role)
                        select h.id, a.id, 'MEMBER'
                        from hubs h join accounts a on a.clerk_user_id = :clerkUserId
                        where h.slug = :hubSlug and a.status = 'ACTIVE'
                        on conflict (hub_id, account_id) do update set
                            status = 'ACTIVE', updated_at = now()
                        """)
                .param("clerkUserId", clerkUserId)
                .param("hubSlug", hubSlug)
                .update();
        if (updated == 0) {
            throw new DomainException("default_hub_not_found", "The default hub is not available");
        }
    }
}
