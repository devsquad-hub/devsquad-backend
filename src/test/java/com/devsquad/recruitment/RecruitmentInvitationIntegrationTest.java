package com.devsquad.recruitment;

import static org.assertj.core.api.Assertions.assertThat;

import com.devsquad.recruitment.application.port.RecruitmentStore;
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
class RecruitmentInvitationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.4-alpine");

    @Autowired RecruitmentStore store;
    @Autowired JdbcTemplate jdbc;

    @Test
    void acceptingInvitationDoesNotDemoteProjectAdministrator() {
        var hubId = UUID.randomUUID();
        var projectId = UUID.randomUUID();
        var adminId = UUID.randomUUID();
        var inviterId = UUID.randomUUID();
        var invitationId = UUID.randomUUID();

        seedAccount(adminId, "admin");
        seedAccount(inviterId, "inviter");
        jdbc.update("insert into hubs (id, name, slug) values (?, 'Hub', ?)", hubId, "hub-" + hubId);
        jdbc.update("""
                insert into projects (id, hub_id, name, slug, project_key, summary, status)
                values (?, ?, 'Project', ?, 'PRJ', 'Summary', 'ACTIVE')
                """, projectId, hubId, "project-" + projectId);
        jdbc.update("insert into project_memberships (project_id, account_id, role) values (?, ?, 'ADMIN')",
                projectId, adminId);
        jdbc.update("""
                insert into project_invitations
                    (id, project_id, account_id, invited_by, functional_role, status, expires_at)
                values (?, ?, ?, ?, 'Backend', 'PENDING', now() + interval '1 day')
                """, invitationId, projectId, adminId, inviterId);

        store.respondToInvitation(invitationId, adminId, true);

        assertThat(jdbc.queryForObject(
                "select role from project_memberships where project_id = ? and account_id = ?",
                String.class,
                projectId,
                adminId)).isEqualTo("ADMIN");
    }

    private void seedAccount(UUID id, String clerkId) {
        jdbc.update("insert into accounts (id, clerk_user_id, display_name) values (?, ?, ?)", id, clerkId, clerkId);
    }
}
