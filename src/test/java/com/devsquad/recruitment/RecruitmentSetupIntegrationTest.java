package com.devsquad.recruitment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devsquad.recruitment.application.RecruitmentService;
import com.devsquad.shared.persistence.JdbcClient;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RecruitmentSetupIntegrationTest {

  @Inject RecruitmentService service;
  @Inject JdbcClient jdbc;

  @Test
  @TestTransaction
  void setupCreatesAndOpensRoundAndPositionAtomically() {
    var seeded = seedProject();

    var result =
        service.setup(
            seeded.clerkId(),
            seeded.projectId(),
            new RecruitmentService.RoundCommand("Seleção", "Contribuidores", null, null),
            new RecruitmentService.PositionCommand(
                "Backend Java", "Construa APIs", List.of("Java", "Postgres"), 2, List.of()));

    assertThat(result.roundId()).isNotNull();
    assertThat(result.positionId()).isNotNull();
    assertThat(jdbc.sql("select status from recruitment_rounds where id = :id")
            .param("id", result.roundId())
            .query(String.class)
            .single())
        .isEqualTo("OPEN");
    assertThat(jdbc.sql("select status from recruitment_positions where id = :id")
            .param("id", result.positionId())
            .query(String.class)
            .single())
        .isEqualTo("OPEN");
    assertThat(jdbc.sql("select status from projects where id = :id")
            .param("id", seeded.projectId())
            .query(String.class)
            .single())
        .isEqualTo("RECRUITING");
  }

  @Test
  @TestTransaction
  void invalidQuestionRollsBackTheWholeSetup() {
    var seeded = seedProject();

    assertThatThrownBy(
            () ->
                service.setup(
                    seeded.clerkId(),
                    seeded.projectId(),
                    new RecruitmentService.RoundCommand("Seleção", null, null, null),
                    new RecruitmentService.PositionCommand(
                        "Backend Java",
                        null,
                        List.of(),
                        1,
                        List.of(
                            new RecruitmentService.Question(
                                "bad", "Invalid", "UNKNOWN", true, List.of())))))
        .hasMessageContaining("Recruitment question type is invalid");

    assertThat(jdbc.sql("select count(*) from recruitment_rounds where project_id = :id")
            .param("id", seeded.projectId())
            .query(Integer.class)
            .single())
        .isZero();
  }

  @Test
  void failureOpeningTheRoundRollsBackTheRoundAndPosition() {
    var seeded = seedProject("ACTIVE");

    assertThatThrownBy(
            () ->
                service.setup(
                    seeded.clerkId(),
                    seeded.projectId(),
                    new RecruitmentService.RoundCommand("Seleção", null, null, null),
                    new RecruitmentService.PositionCommand(
                        "Backend Java", null, List.of(), 1, List.of())))
        .hasMessageContaining("Round or project is not in an openable state");

    assertThat(jdbc.sql("select count(*) from recruitment_rounds where project_id = :id")
            .param("id", seeded.projectId())
            .query(Integer.class)
            .single())
        .isZero();
    assertThat(jdbc.sql("select count(*) from recruitment_positions rp join recruitment_rounds rr on rr.id = rp.round_id where rr.project_id = :id")
            .param("id", seeded.projectId())
            .query(Integer.class)
            .single())
        .isZero();
  }

  private SeededProject seedProject() {
    return seedProject("PLANNING");
  }

  private SeededProject seedProject(String status) {
    var hubId = UUID.randomUUID();
    var projectId = UUID.randomUUID();
    var accountId = UUID.randomUUID();
    var clerkId = "recruitment-admin-" + accountId;
    var projectKey = "S" + accountId.toString().replace("-", "").substring(0, 11);
    jdbc.sql("insert into accounts (id, clerk_user_id, display_name) values (:id, :clerk, 'Admin')")
        .param("id", accountId)
        .param("clerk", clerkId)
        .update();
    jdbc.sql("insert into hubs (id, name, slug) values (:id, 'Hub', :slug)")
        .param("id", hubId)
        .param("slug", "recruitment-setup-hub-" + hubId)
        .update();
    jdbc.sql(
            "insert into projects (id, hub_id, name, slug, project_key, summary, status) values (:id, :hub, 'Project', :slug, :projectKey, 'Summary', :status)")
        .param("id", projectId)
        .param("hub", hubId)
        .param("slug", "recruitment-setup-project-" + projectId)
        .param("projectKey", projectKey)
        .param("status", status)
        .update();
    jdbc.sql(
            "insert into project_memberships (project_id, account_id, role) values (:project, :account, 'ADMIN')")
        .param("project", projectId)
        .param("account", accountId)
        .update();
    return new SeededProject(projectId, clerkId);
  }

  private record SeededProject(UUID projectId, String clerkId) {}
}
