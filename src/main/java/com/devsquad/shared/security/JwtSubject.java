package com.devsquad.shared.security;

import com.devsquad.shared.domain.DomainException;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtSubject {
    private JwtSubject() {}

    public static String subject(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new DomainException("authentication_required", "Authentication is required");
        }
        return jwt.getSubject();
    }
}
