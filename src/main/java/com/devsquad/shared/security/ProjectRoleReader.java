package com.devsquad.shared.security;

import com.devsquad.project.domain.ProjectRole;
import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface ProjectRoleReader {
    Optional<ProjectRole> findActiveProjectRole(UUID projectId, UUID accountId);
}
