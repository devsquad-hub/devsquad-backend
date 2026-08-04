package com.devsquad.proposal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devsquad.shared.domain.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectProposalTest {

  @Test
  void rejectedProposalCanBeEditedAndResubmitted() {
    var proposal =
        ProjectProposal.draft(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "API comunitária", "Resumo");

    proposal.submit();
    proposal.reject(UUID.randomUUID(), "Detalhe melhor o impacto");
    proposal.edit("API comunitária v2", "Resumo revisado");
    proposal.submit();

    assertThat(proposal.status()).isEqualTo(ProposalStatus.PENDING);
    assertThat(proposal.title()).isEqualTo("API comunitária v2");
    assertThat(proposal.decisionReason()).isNull();
  }

  @Test
  void approvedProposalCannotBeChangedAgain() {
    var proposal =
        ProjectProposal.draft(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Projeto", "Resumo");
    proposal.submit();
    proposal.approve(UUID.randomUUID(), UUID.randomUUID());

    assertThatThrownBy(() -> proposal.edit("Outro", "Outro resumo"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("approved");
  }
}
