package com.devsquad.identity.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AuthorizedPartyValidatorTest {

    private final AuthorizedPartyValidator validator = new AuthorizedPartyValidator(Set.of("https://devsquad.example"));

    @Test
    void acceptsConfiguredAuthorizedParty() {
        assertThat(validator.validate(token("https://devsquad.example")).hasErrors()).isFalse();
    }

    @Test
    void rejectsUnexpectedAuthorizedParty() {
        assertThat(validator.validate(token("https://attacker.example")).hasErrors()).isTrue();
    }

    private static Jwt token(String authorizedParty) {
        var now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user_123")
                .claim("azp", authorizedParty)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();
    }
}
