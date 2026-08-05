package com.devsquad.recruitment;

import static org.assertj.core.api.Assertions.assertThat;

import com.devsquad.recruitment.adapter.out.persistence.JdbcRecruitmentStore;
import com.devsquad.shared.persistence.JdbcClient;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PublicRecruitmentVisibilityIntegrationTest {

  @Inject JdbcClient jdbc;
  @Inject JdbcRecruitmentStore recruitment;

  @Test
  @TestTransaction
  void archivedProjectPositionsAreNotPublicOrApplicable() {
    var hubId = UUID.randomUUID();
    var projectId = UUID.randomUUID();
    var roundId = UUID.randomUUID();
    var positionId = UUID.randomUUID();
    jdbc.sql("insert into hubs (id, name, slug) values (:id, 'Hub', :slug)")
        .param("id", hubId)
        .param("slug", "recruitment-hub-" + hubId)
        .update();
    jdbc.sql(
            "insert into projects (id, hub_id, name, slug, project_key, summary, status) values (:id, :hub, 'Archived', :slug, 'ARC', 'Summary', 'ARCHIVED')")
        .param("id", projectId)
        .param("hub", hubId)
        .param("slug", "recruitment-project-" + projectId)
        .update();
    jdbc.sql(
            "insert into recruitment_rounds (id, project_id, name, status, opens_at) values (:id, :project, 'Round', 'OPEN', now() - interval '1 hour')")
        .param("id", roundId)
        .param("project", projectId)
        .update();
    jdbc.sql(
            "insert into recruitment_positions (id, round_id, title, capacity, status) values (:id, :round, 'Position', 1, 'OPEN')")
        .param("id", positionId)
        .param("round", roundId)
        .update();

    assertThat(recruitment.publicPositions(projectId)).isEmpty();
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> recruitment.positionContext(positionId))
        .hasMessageContaining("Recruitment position is not open");
  }
}
