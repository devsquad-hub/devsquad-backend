package com.devsquad.shared.security;

import com.devsquad.shared.domain.DomainException;
import jakarta.ws.rs.core.SecurityContext;

public final class JwtSubject {
  private JwtSubject() {}

  public static String subject(SecurityContext context) {
    if (context == null
        || context.getUserPrincipal() == null
        || context.getUserPrincipal().getName() == null
        || context.getUserPrincipal().getName().isBlank()) {
      throw new DomainException("authentication_required", "Authentication is required");
    }
    return context.getUserPrincipal().getName();
  }
}
