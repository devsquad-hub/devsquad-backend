package com.devsquad.hub.adapter.out.persistence;

import com.devsquad.shared.domain.DomainException;
import com.devsquad.shared.persistence.JdbcClient;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class BootstrapConfiguration {

  private final JdbcClient jdbc;
  private final String hubName;
  private final String hubSlug;
  private final String masterClerkUserId;
  private final boolean enabled;

  public BootstrapConfiguration(
      JdbcClient jdbc,
      @ConfigProperty(name = "app.bootstrap.hub-name") Optional<String> hubName,
      @ConfigProperty(name = "app.bootstrap.hub-slug") Optional<String> hubSlug,
      @ConfigProperty(name = "app.bootstrap.master-clerk-user-id")
          Optional<String> masterClerkUserId,
      @ConfigProperty(name = "app.bootstrap.enabled") boolean enabled) {
    this.jdbc = jdbc;
    this.hubName = hubName.orElse("");
    this.hubSlug = hubSlug.orElse("");
    this.masterClerkUserId = masterClerkUserId.orElse("");
    this.enabled = enabled;
  }

  @Transactional
  void onStart(@Observes StartupEvent event) {
    if (!enabled) return;
    requireConfiguration();
    jdbc.sql(
            """
            insert into accounts (clerk_user_id, display_name)
            values (:clerkId, 'DevSquad master')
            on conflict (clerk_user_id) do nothing
            """)
        .param("clerkId", masterClerkUserId)
        .update();
    jdbc.sql(
            """
            insert into hubs (name, slug) values (:name, :slug)
            on conflict (slug) do update set name = excluded.name, updated_at = now()
            """)
        .param("name", hubName)
        .param("slug", hubSlug)
        .update();
    jdbc.sql(
            """
            insert into hub_memberships (hub_id, account_id, role)
            select h.id, a.id, 'MEMBER'
            from hubs h cross join accounts a
            where h.slug = :slug and a.status = 'ACTIVE'
            on conflict (hub_id, account_id) do update set status = 'ACTIVE', updated_at = now()
            """)
        .param("slug", hubSlug)
        .update();
    jdbc.sql(
            """
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
      throw new DomainException(
          "invalid_bootstrap_configuration",
          "BOOTSTRAP_HUB_NAME, BOOTSTRAP_HUB_SLUG and BOOTSTRAP_MASTER_CLERK_USER_ID are required");
    }
  }
}
