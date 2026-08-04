package com.devsquad.identity.domain;

import com.devsquad.shared.domain.DomainException;
import java.util.LinkedHashSet;
import java.util.List;

public record AccountProfile(
    String displayName,
    String bio,
    List<String> skills,
    String githubUrl,
    String linkedinUrl,
    String portfolioUrl,
    Integer availabilityHours) {

  public AccountProfile {
    if (displayName == null || displayName.isBlank()) {
      throw new DomainException("invalid_display_name", "Display name must not be blank");
    }
    if (availabilityHours != null && (availabilityHours < 0 || availabilityHours > 168)) {
      throw new DomainException(
          "invalid_availability_hours", "Availability must be between 0 and 168 hours");
    }
    displayName = displayName.trim();
    bio = trimToNull(bio);
    skills = normalizeSkills(skills);
    githubUrl = trimToNull(githubUrl);
    linkedinUrl = trimToNull(linkedinUrl);
    portfolioUrl = trimToNull(portfolioUrl);
  }

  public static AccountProfile create(
      String displayName,
      String bio,
      List<String> skills,
      String githubUrl,
      String linkedinUrl,
      String portfolioUrl,
      Integer availabilityHours) {
    return new AccountProfile(
        displayName, bio, skills, githubUrl, linkedinUrl, portfolioUrl, availabilityHours);
  }

  private static List<String> normalizeSkills(List<String> values) {
    if (values == null) {
      return List.of();
    }
    var normalized = new LinkedHashSet<String>();
    for (var value : values) {
      if (value != null && !value.isBlank()) {
        var candidate = value.trim();
        if (normalized.stream().noneMatch(existing -> existing.equalsIgnoreCase(candidate))) {
          normalized.add(candidate);
        }
      }
    }
    if (normalized.size() > 30) {
      throw new DomainException("too_many_skills", "A profile can have at most 30 skills");
    }
    return List.copyOf(normalized);
  }

  private static String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
