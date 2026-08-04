package com.devsquad.identity.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthorizedPartyAugmentorTest {

  @Test
  void acceptsAnyConfiguredAuthorizedParty() {
    var allowed = Set.of("https://app.example.com", "http://localhost:3000");

    assertThat(AuthorizedPartyAugmentor.isAuthorized("https://app.example.com", allowed)).isTrue();
    assertThat(AuthorizedPartyAugmentor.isAuthorized("http://localhost:3000", allowed)).isTrue();
  }

  @Test
  void rejectsMissingOrUnknownAuthorizedParty() {
    var allowed = Set.of("https://app.example.com");

    assertThat(AuthorizedPartyAugmentor.isAuthorized(null, allowed)).isFalse();
    assertThat(AuthorizedPartyAugmentor.isAuthorized("https://evil.example", allowed)).isFalse();
  }
}
