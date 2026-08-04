package com.devsquad.project.application.port;

import com.devsquad.project.domain.ProjectStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectStore {
  Optional<ProjectView> find(UUID projectId);

  List<ProjectView> findPublic(UUID hubId);

  Optional<ProjectView> findPublicBySlug(String hubSlug, String projectSlug);

  ProjectView updateStatus(UUID projectId, ProjectStatus expectedStatus, ProjectStatus status);

  ProjectView update(UUID projectId, ProjectUpdate update);

  void assignAdmin(UUID projectId, UUID accountId);

  boolean isActiveHubMember(UUID hubId, UUID accountId);

  record ProjectView(
      UUID id,
      UUID hubId,
      String name,
      String slug,
      String projectKey,
      String summary,
      String description,
      ProjectStatus status,
      String repositoryUrl,
      String communicationUrl,
      List<String> tags,
      long totalTasks,
      long completedTasks,
      List<MemberView> members) {}

  record MemberView(
      UUID accountId, String displayName, String avatarUrl, String role, String functionalRole) {}

  record ProjectUpdate(
      String name,
      String summary,
      String description,
      String repositoryUrl,
      String communicationUrl,
      List<String> tags) {}
}
