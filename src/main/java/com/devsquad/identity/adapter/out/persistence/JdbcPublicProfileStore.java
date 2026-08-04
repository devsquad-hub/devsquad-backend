package com.devsquad.identity.adapter.out.persistence;

import com.devsquad.identity.application.PublicProfileService.ProjectSummary;
import com.devsquad.identity.application.PublicProfileService.PublicProfile;
import com.devsquad.identity.application.port.PublicProfileStore;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPublicProfileStore implements PublicProfileStore {
    private final JdbcClient jdbc;

    public JdbcPublicProfileStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<PublicProfile> find(UUID accountId) {
        return jdbc.sql("""
                        select id, display_name, avatar_url, bio, skills, github_url, linkedin_url,
                               portfolio_url, availability_hours from accounts where id = :id and status = 'ACTIVE'
                        """).param("id", accountId)
                .query((rs, row) -> new PublicProfile(rs.getObject("id", UUID.class), rs.getString("display_name"),
                        rs.getString("avatar_url"), rs.getString("bio"),
                        List.copyOf(Arrays.asList((String[]) rs.getArray("skills").getArray())),
                        rs.getString("github_url"), rs.getString("linkedin_url"), rs.getString("portfolio_url"),
                        rs.getObject("availability_hours", Integer.class), projects(accountId)))
                .optional();
    }

    private List<ProjectSummary> projects(UUID accountId) {
        return jdbc.sql("""
                        select p.name, p.slug, h.slug as hub_slug, p.summary, p.status, pm.functional_role
                        from project_memberships pm join projects p on p.id = pm.project_id join hubs h on h.id = p.hub_id
                        where pm.account_id = :account and pm.status = 'ACTIVE' and p.status <> 'ARCHIVED'
                        order by p.updated_at desc
                        """).param("account", accountId)
                .query((rs, row) -> new ProjectSummary(rs.getString("name"), rs.getString("slug"),
                        rs.getString("hub_slug"), rs.getString("summary"), rs.getString("status"),
                        rs.getString("functional_role"))).list();
    }
}
