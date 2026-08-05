package com.devsquad.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.devsquad.hub.adapter.out.persistence.JdbcHubMembershipStore;
import com.devsquad.identity.adapter.out.persistence.JdbcAccountStore;
import com.devsquad.project.adapter.out.persistence.JdbcProjectStore;
import com.devsquad.shared.persistence.JdbcClient;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PublicVisibilityIntegrationTest {

  @Inject JdbcClient jdbc;
  @Inject JdbcProjectStore projects;
  @Inject JdbcHubMembershipStore memberships;
  @Inject JdbcAccountStore accounts;

  @Test
  @TestTransaction
  void archivedProjectsAreNotPublicAndCatalogDoesNotLoadMembersPerProject() {
    var hubId = UUID.randomUUID();
    var projectId = UUID.randomUUID();
    var accountId = UUID.randomUUID();
    seedAccount(accountId, "archived-member-" + accountId);
    jdbc.sql("insert into hubs (id, name, slug) values (:id, 'Hub', :slug)")
        .param("id", hubId)
        .param("slug", "hub-" + hubId)
        .update();
    jdbc.sql(
            """
            insert into projects (id, hub_id, name, slug, project_key, summary, status)
            values (:id, :hub, 'Archived', :slug, 'ARC', 'Not public', 'ARCHIVED')
            """)
        .param("id", projectId)
        .param("hub", hubId)
        .param("slug", "archived-" + projectId)
        .update();
    jdbc.sql(
            "insert into project_memberships (project_id, account_id, role) values (:project, :account, 'MEMBER')")
        .param("project", projectId)
        .param("account", accountId)
        .update();

    assertThat(projects.findPublic(hubId)).isEmpty();
    assertThat(projects.findPublicBySlug("hub-" + hubId, "archived-" + projectId)).isEmpty();
    assertThat(projects.find(projectId).orElseThrow().members()).hasSize(1);
  }

  @Test
  @TestTransaction
  void deletedAccountsAreExcludedFromHubAndProjectTeams() {
    var hubId = UUID.randomUUID();
    var projectId = UUID.randomUUID();
    var accountId = UUID.randomUUID();
    seedAccount(accountId, "deleted-member-" + accountId);
    jdbc.sql("insert into hubs (id, name, slug) values (:id, 'Hub', :slug)")
        .param("id", hubId)
        .param("slug", "hub-" + hubId)
        .update();
    jdbc.sql(
            "insert into projects (id, hub_id, name, slug, project_key, summary, status) values (:id, :hub, 'Project', :slug, 'PRJ', 'Summary', 'ACTIVE')")
        .param("id", projectId)
        .param("hub", hubId)
        .param("slug", "project-" + projectId)
        .update();
    jdbc.sql(
            "insert into hub_memberships (hub_id, account_id, role) values (:hub, :account, 'MEMBER')")
        .param("hub", hubId)
        .param("account", accountId)
        .update();
    jdbc.sql(
            "insert into project_memberships (project_id, account_id, role) values (:project, :account, 'MEMBER')")
        .param("project", projectId)
        .param("account", accountId)
        .update();
    accounts.markDeleted("deleted-member-" + accountId);

    assertThat(memberships.findMembers(hubId)).isEmpty();
    assertThat(projects.find(projectId).orElseThrow().members()).isEmpty();
    assertThat(activeMemberships(accountId)).isZero();
  }

  @Test
  @TestTransaction
  void publicCatalogLeavesTeamExpansionToTheProjectDetail() {
    var hubId = UUID.randomUUID();
    var projectId = UUID.randomUUID();
    var accountId = UUID.randomUUID();
    seedAccount(accountId, "catalog-member-" + accountId);
    jdbc.sql("insert into hubs (id, name, slug) values (:id, 'Hub', :slug)")
        .param("id", hubId)
        .param("slug", "catalog-hub-" + hubId)
        .update();
    jdbc.sql(
            "insert into projects (id, hub_id, name, slug, project_key, summary, status) values (:id, :hub, 'Project', :slug, 'CAT', 'Summary', 'ACTIVE')")
        .param("id", projectId)
        .param("hub", hubId)
        .param("slug", "catalog-project-" + projectId)
        .update();
    jdbc.sql(
            "insert into project_memberships (project_id, account_id, role) values (:project, :account, 'MEMBER')")
        .param("project", projectId)
        .param("account", accountId)
        .update();

    assertThat(projects.findPublic(hubId)).singleElement().satisfies(project -> {
      assertThat(project.members()).isEmpty();
    });
    assertThat(projects.find(projectId).orElseThrow().members()).hasSize(1);
  }

  private int activeMemberships(UUID accountId) {
    return jdbc.sql(
            "select count(*) from hub_memberships where account_id = :id and status = 'ACTIVE'")
        .param("id", accountId)
        .query(Integer.class)
        .single()
        + jdbc.sql(
                "select count(*) from project_memberships where account_id = :id and status = 'ACTIVE'")
            .param("id", accountId)
            .query(Integer.class)
            .single();
  }

  private void seedAccount(UUID id, String clerkId) {
    jdbc.sql("insert into accounts (id, clerk_user_id, display_name) values (:id, :clerk, 'Member')")
        .param("id", id)
        .param("clerk", clerkId)
        .update();
  }
}
