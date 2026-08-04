package com.devsquad.hub.adapter.out.persistence;

import com.devsquad.hub.application.port.HubCatalog;
import com.devsquad.hub.domain.Hub;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcHubCatalog implements HubCatalog {
    private final JdbcClient jdbc;

    JdbcHubCatalog(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public List<Hub> findAll() {
        return jdbc.sql("select id, name, slug, description from hubs order by name")
                .query((rs, row) -> new Hub(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getString("slug"), rs.getString("description"))).list();
    }

    @Override
    public Optional<Hub> findBySlug(String slug) {
        return jdbc.sql("select id, name, slug, description from hubs where slug = :slug")
                .param("slug", slug).query((rs, row) -> new Hub(rs.getObject("id", UUID.class),
                        rs.getString("name"), rs.getString("slug"), rs.getString("description"))).optional();
    }
}
