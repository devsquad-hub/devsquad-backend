package com.devsquad.identity.domain;

import java.util.List;
import java.util.UUID;

public record Account(
        UUID id,
        String clerkUserId,
        String email,
        String displayName,
        String avatarUrl,
        String bio,
        List<String> skills,
        String githubUrl,
        String linkedinUrl,
        String portfolioUrl,
        Integer availabilityHours) {}
