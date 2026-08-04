package com.devsquad.shared.security;

import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface AccountIdentity {
  Optional<UUID> findActiveAccountId(String clerkUserId);
}
