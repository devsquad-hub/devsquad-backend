package com.devsquad.recruitment;

import static org.assertj.core.api.Assertions.assertThat;

import com.devsquad.recruitment.application.port.RecruitmentStore;
import com.devsquad.shared.persistence.JdbcClient;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RecruitmentInvitationIntegrationTest {

  @Inject RecruitmentStore store;
  @Inject JdbcClient jdbc;

  @Test
  @TestTransaction
  void acceptingInvitationDoesNotDemoteProjectAdministrator() {
    var hubId = UUID.randomUUID();
    var projectId = UUID.randomUUID();
    var adminId = UUID.randomUUID();
    var inviterId = UUID.randomUUID();
    var invitationId = UUID.randomUUID();

    seedAccount(adminId, "admin");
    seedAccount(inviterId, "inviter");
    jdbc.sql("insert into hubs (id, name, slug) values (:id, 'Hub', :slug)")
        .param("id", hubId)
        .param("slug", "hub-" + hubId)
        .update();
    jdbc.sql(
            """
            insert into projects (id, hub_id, name, slug, project_key, summary, status)
            values (:id, :hub, 'Project', :slug, 'PRJ', 'Summary', 'ACTIVE')
            """)
        .param("id", projectId)
        .param("hub", hubId)
        .param("slug", "project-" + projectId)
        .update();
    jdbc.sql(
            "insert into project_memberships (project_id, account_id, role) values (:project,"
                + " :account, 'ADMIN')")
        .param("project", projectId)
        .param("account", adminId)
        .update();
    jdbc.sql(
            """
            insert into project_invitations
                (id, project_id, account_id, invited_by, functional_role, status, expires_at)
            values (:id, :project, :account, :inviter, 'Backend', 'PENDING', now() + interval '1 day')
            """)
        .param("id", invitationId)
        .param("project", projectId)
        .param("account", adminId)
        .param("inviter", inviterId)
        .update();

    store.respondToInvitation(invitationId, adminId, true);

    assertThat(
            jdbc.sql(
                    """
                    select role from project_memberships where project_id = :project and account_id = :account
                    """)
                .param("project", projectId)
                .param("account", adminId)
                .query(String.class)
                .single())
        .isEqualTo("ADMIN");
  }

  private void seedAccount(UUID id, String clerkId) {
    jdbc.sql("insert into accounts (id, clerk_user_id, display_name) values (:id, :clerk, :name)")
        .param("id", id)
        .param("clerk", clerkId)
        .param("name", clerkId)
        .update();
  }
}
