package com.devsquad.hub.adapter.out.persistence;

import com.devsquad.hub.application.port.HubMembershipStore;
import com.devsquad.hub.domain.HubRole;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcHubMembershipStore implements HubMembershipStore {

    private final JdbcClient jdbc;

    public JdbcHubMembershipStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<MembershipView> findByAccount(UUID accountId) {
        return jdbc.sql("""
                        select h.id, h.name, h.slug, hm.role from hub_memberships hm
                        join hubs h on h.id = hm.hub_id
                        where hm.account_id = :accountId and hm.status = 'ACTIVE' order by h.name
                        """)
                .param("accountId", accountId)
                .query((rs, row) -> new MembershipView(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getString("slug"), HubRole.valueOf(rs.getString("role"))))
                .list();
    }

    @Override
    public List<MemberView> findMembers(UUID hubId) {
        return jdbc.sql("""
                        select a.id, a.display_name, a.email, a.avatar_url, hm.role
                        from hub_memberships hm join accounts a on a.id = hm.account_id
                        where hm.hub_id = :hubId and hm.status = 'ACTIVE'
                        order by case hm.role when 'MASTER' then 0 when 'ADMIN' then 1 else 2 end, a.display_name
                        """)
                .param("hubId", hubId)
                .query((rs, row) -> new MemberView(rs.getObject("id", UUID.class), rs.getString("display_name"),
                        rs.getString("email"), rs.getString("avatar_url"), HubRole.valueOf(rs.getString("role"))))
                .list();
    }

    @Override
    public void assign(UUID hubId, UUID accountId, HubRole role) {
        jdbc.sql("""
                        insert into hub_memberships (hub_id, account_id, role)
                        values (:hubId, :accountId, :role)
                        on conflict (hub_id, account_id) do update set role = excluded.role, status = 'ACTIVE', updated_at = now()
                        """)
                .param("hubId", hubId)
                .param("accountId", accountId)
                .param("role", role.name())
                .update();
    }

    @Override
    public boolean isActiveAccount(UUID accountId) {
        return jdbc.sql("select count(*) from accounts where id = :id and status = 'ACTIVE'")
                .param("id", accountId).query(Integer.class).single() > 0;
    }
}
