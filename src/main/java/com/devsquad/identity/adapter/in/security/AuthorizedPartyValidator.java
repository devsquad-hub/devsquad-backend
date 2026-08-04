package com.devsquad.identity.adapter.in.security;

import java.util.Set;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public final class AuthorizedPartyValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_AZP =
            new OAuth2Error("invalid_token", "JWT authorized party is not allowed", null);

    private final Set<String> authorizedParties;

    public AuthorizedPartyValidator(Set<String> authorizedParties) {
        this.authorizedParties = Set.copyOf(authorizedParties);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        var authorizedParty = token.getClaimAsString("azp");
        return authorizedParty != null && authorizedParties.contains(authorizedParty)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(INVALID_AZP);
    }
}
