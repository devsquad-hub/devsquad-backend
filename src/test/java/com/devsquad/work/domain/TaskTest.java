package com.devsquad.work.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devsquad.shared.domain.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskTest {

  @Test
  void movingTaskRequiresCurrentVersion() {
    var firstColumn = UUID.randomUUID();
    var secondColumn = UUID.randomUUID();
    var task = Task.create(UUID.randomUUID(), UUID.randomUUID(), 42, "Corrigir login", firstColumn);

    task.moveTo(secondColumn, 0);

    assertThat(task.columnId()).isEqualTo(secondColumn);
    assertThat(task.version()).isEqualTo(1);
    assertThatThrownBy(() -> task.moveTo(firstColumn, 0))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("version");
  }
}
