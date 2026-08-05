package com.devsquad.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.devsquad.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

class ApiExceptionHandlerTest {

  @Test
  void reportsMissingDefaultHubAsTemporaryServiceUnavailable() {
    var response =
        new ApiExceptionHandler()
            .toResponse(new DomainException("default_hub_not_found", "The default hub is not available"));

    assertThat(response.getStatus()).isEqualTo(503);
  }
}
