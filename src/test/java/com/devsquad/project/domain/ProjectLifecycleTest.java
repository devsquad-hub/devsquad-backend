package com.devsquad.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devsquad.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

class ProjectLifecycleTest {

  @Test
  void followsPlanningRecruitingActiveCompletedArchivedFlow() {
    var lifecycle = new ProjectLifecycle(ProjectStatus.PLANNING);

    lifecycle.transitionTo(ProjectStatus.RECRUITING);
    lifecycle.transitionTo(ProjectStatus.ACTIVE);
    lifecycle.transitionTo(ProjectStatus.COMPLETED);
    lifecycle.transitionTo(ProjectStatus.ARCHIVED);

    assertThat(lifecycle.status()).isEqualTo(ProjectStatus.ARCHIVED);
  }

  @Test
  void cannotReactivateCompletedProject() {
    var lifecycle = new ProjectLifecycle(ProjectStatus.COMPLETED);

    assertThatThrownBy(() -> lifecycle.transitionTo(ProjectStatus.ACTIVE))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("COMPLETED");
  }
}
