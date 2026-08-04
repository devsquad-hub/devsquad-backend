package com.devsquad.proposal;

import static org.assertj.core.api.Assertions.assertThat;

import com.devsquad.proposal.application.ProposalService;
import com.devsquad.proposal.application.port.ProposalStore;
import com.devsquad.shared.persistence.JdbcClient;
import com.devsquad.work.application.WorkService;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProposalFlowIntegrationTest {

  @Inject ProposalService service;
  @Inject WorkService workService;
  @Inject JdbcClient jdbc;

  @Test
  @TestTransaction
  void proposalApprovalCreatesProjectAdminAndDefaultWorkflow() {
    var hubId = UUID.randomUUID();
    var authorId = UUID.randomUUID();
    var masterId = UUID.randomUUID();
    seedAccount(authorId, "author", "Author");
    seedAccount(masterId, "master", "Master");
    jdbc.sql("insert into hubs (id, name, slug) values (:id, 'Hub', :slug)")
        .param("id", hubId)
        .param("slug", "hub-" + hubId)
        .update();
    jdbc.sql(
            """
            insert into hub_memberships (hub_id, account_id, role)
            values (:hub, :author, 'MEMBER'), (:hub, :master, 'MASTER')
            """)
        .param("hub", hubId)
        .param("author", authorId)
        .param("master", masterId)
        .update();

    var content =
        new ProposalStore.ProposalContent(
            "API Comunitária", "Uma API útil", null, null, null, List.of("Java"));
    var draft = service.create("author", hubId, content);
    service.submit("author", draft.id());
    var approved = service.approve("master", draft.id());

    assertThat(approved.status()).isEqualTo("APPROVED");
    assertThat(count("projects", "id", approved.projectId())).isEqualTo(1);
    assertThat(
            jdbc.sql(
                    "select count(*) from project_memberships where project_id = :id and role ="
                        + " 'ADMIN'")
                .param("id", approved.projectId())
                .query(Integer.class)
                .single())
        .isEqualTo(1);
    assertThat(count("workflow_columns", "project_id", approved.projectId())).isEqualTo(4);

    var backlog = column(approved.projectId(), "BACKLOG");
    var started = column(approved.projectId(), "STARTED");
    var task =
        workService.createTask(
            "author",
            approved.projectId(),
            new WorkService.TaskCommand(
                null,
                backlog,
                null,
                "Primeira tarefa",
                null,
                "HIGH",
                null,
                null,
                0,
                List.of(authorId)));
    var moved = workService.move("author", task.id(), started, 0, task.version());

    assertThat(moved.version()).isEqualTo(1);
    assertThat(workService.board("author", approved.projectId()).columns()).hasSize(4);
  }

  private void seedAccount(UUID id, String clerkId, String name) {
    jdbc.sql("insert into accounts (id, clerk_user_id, display_name) values (:id, :clerk, :name)")
        .param("id", id)
        .param("clerk", clerkId)
        .param("name", name)
        .update();
  }

  private int count(String table, String column, UUID id) {
    return jdbc.sql("select count(*) from " + table + " where " + column + " = :id")
        .param("id", id)
        .query(Integer.class)
        .single();
  }

  private UUID column(UUID projectId, String group) {
    return jdbc.sql(
            "select id from workflow_columns where project_id = :project and semantic_group ="
                + " :group")
        .param("project", projectId)
        .param("group", group)
        .query(UUID.class)
        .single();
  }
}
