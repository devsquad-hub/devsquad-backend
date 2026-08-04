package com.devsquad.proposal;

import static org.assertj.core.api.Assertions.assertThat;

import com.devsquad.proposal.application.ProposalService;
import com.devsquad.proposal.application.port.ProposalStore;
import com.devsquad.work.application.WorkService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = {
        "app.security.enabled=false",
        "app.bootstrap.enabled=false",
        "app.storage.initialize-bucket=false"
})
@Transactional
class ProposalFlowIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.4-alpine");

    @Autowired ProposalService service;
    @Autowired WorkService workService;
    @Autowired JdbcTemplate jdbc;

    @Test
    void proposalApprovalCreatesProjectAdminAndDefaultWorkflow() {
        var hubId = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        var masterId = UUID.randomUUID();
        seedAccount(authorId, "author", "Author");
        seedAccount(masterId, "master", "Master");
        jdbc.update("insert into hubs (id, name, slug) values (?, 'Hub', ?)", hubId, "hub-" + hubId);
        jdbc.update("insert into hub_memberships (hub_id, account_id, role) values (?, ?, 'MEMBER'), (?, ?, 'MASTER')",
                hubId, authorId, hubId, masterId);

        var content = new ProposalStore.ProposalContent("API Comunitária", "Uma API útil", null, null, null, List.of("Java"));
        var draft = service.create("author", hubId, content);
        service.submit("author", draft.id());
        var approved = service.approve("master", draft.id());

        assertThat(approved.status()).isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("select count(*) from projects where id = ?", Integer.class, approved.projectId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from project_memberships where project_id = ? and role = 'ADMIN'",
                Integer.class, approved.projectId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from workflow_columns where project_id = ?",
                Integer.class, approved.projectId())).isEqualTo(4);

        var backlog = jdbc.queryForObject("select id from workflow_columns where project_id = ? and semantic_group = 'BACKLOG'",
                UUID.class, approved.projectId());
        var started = jdbc.queryForObject("select id from workflow_columns where project_id = ? and semantic_group = 'STARTED'",
                UUID.class, approved.projectId());
        var task = workService.createTask("author", approved.projectId(),
                new WorkService.TaskCommand(null, backlog, null, "Primeira tarefa", null, "HIGH", null, null, 0, List.of(authorId)));
        var moved = workService.move("author", task.id(), started, 0, task.version());

        assertThat(moved.version()).isEqualTo(1);
        assertThat(workService.board("author", approved.projectId()).columns()).hasSize(4);
    }

    private void seedAccount(UUID id, String clerkId, String name) {
        jdbc.update("insert into accounts (id, clerk_user_id, display_name) values (?, ?, ?)", id, clerkId, name);
    }
}
