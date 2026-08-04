package com.devsquad.recruitment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devsquad.shared.domain.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectApplicationTest {

    @Test
    void acceptedApplicationCannotReceiveAnotherDecision() {
        var application = ProjectApplication.submitted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        application.accept(UUID.randomUUID(), "Bem-vindo");

        assertThat(application.status()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThatThrownBy(() -> application.reject(UUID.randomUUID(), "Mudamos de ideia"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("already decided");
    }
}
