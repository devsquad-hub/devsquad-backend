package com.devsquad.identity.adapter.in.security;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
public class AuthorizedPartyAugmentor implements SecurityIdentityAugmentor {

  private final Set<String> authorizedParties;

  public AuthorizedPartyAugmentor(
      @ConfigProperty(name = "app.clerk.authorized-parties") Set<String> authorizedParties) {
    this.authorizedParties =
        authorizedParties.stream()
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public Uni<SecurityIdentity> augment(
      SecurityIdentity identity, AuthenticationRequestContext context) {
    if (identity.isAnonymous() || !(identity.getPrincipal() instanceof JsonWebToken token)) {
      return Uni.createFrom().item(identity);
    }
    Object authorizedParty = token.getClaim("azp");
    if (isAuthorized(authorizedParty, authorizedParties)) {
      return Uni.createFrom().item(identity);
    }
    return Uni.createFrom().failure(new AuthenticationFailedException("Unauthorized token party"));
  }

  static boolean isAuthorized(Object authorizedParty, Set<String> allowed) {
    return authorizedParty instanceof String value && allowed.contains(value);
  }
}
