package com.devsquad.identity.application.port;

@FunctionalInterface
public interface DefaultHubMembership {
  void ensureFor(String clerkUserId);
}
