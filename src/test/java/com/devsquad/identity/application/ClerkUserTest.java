package com.devsquad.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ClerkUserTest {

  @Test
  void choosesPrimaryEmailAndBuildsDisplayName() {
    var user =
        new ClerkUser(
            "user_123",
            "email_2",
            "Ada",
            "Lovelace",
            "https://img.test/ada.png",
            List.of(
                new ClerkUser.Email("email_1", "old@example.com"),
                new ClerkUser.Email("email_2", "ada@example.com")));

    assertThat(user.primaryEmail()).isEqualTo("ada@example.com");
    assertThat(user.displayName()).isEqualTo("Ada Lovelace");
  }
}
