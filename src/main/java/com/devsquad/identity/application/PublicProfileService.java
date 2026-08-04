package com.devsquad.identity.application;

import com.devsquad.identity.application.port.PublicProfileStore;
import com.devsquad.shared.domain.DomainException;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PublicProfileService {
  private final PublicProfileStore profiles;

  public PublicProfileService(PublicProfileStore profiles) {
    this.profiles = profiles;
  }

  public PublicProfile find(UUID accountId) {
    return profiles
        .find(accountId)
        .orElseThrow(() -> new DomainException("profile_not_found", "Profile was not found"));
  }

  public record PublicProfile(
      UUID id,
      String displayName,
      String avatarUrl,
      String bio,
      List<String> skills,
      String githubUrl,
      String linkedinUrl,
      String portfolioUrl,
      Integer availabilityHours,
      List<ProjectSummary> projects) {}

  public record ProjectSummary(
      String name,
      String slug,
      String hubSlug,
      String summary,
      String status,
      String functionalRole) {}
}
