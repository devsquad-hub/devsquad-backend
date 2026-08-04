package com.devsquad.project.application;

import com.devsquad.project.application.port.ProjectStore;
import com.devsquad.project.domain.ProjectLifecycle;
import com.devsquad.project.domain.ProjectStatus;
import com.devsquad.shared.domain.DomainException;
import com.devsquad.shared.security.AuthorizationService;
import com.devsquad.shared.security.ViewerCapabilities;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProjectService {

  private final ProjectStore projects;
  private final AuthorizationService authorization;

  public ProjectService(ProjectStore projects, AuthorizationService authorization) {
    this.projects = projects;
    this.authorization = authorization;
  }

  public List<ProjectStore.ProjectView> publicProjects(UUID hubId) {
    return projects.findPublic(hubId);
  }

  public ProjectStore.ProjectView publicProject(String hubSlug, String projectSlug) {
    return projects
        .findPublicBySlug(hubSlug, projectSlug)
        .orElseThrow(() -> new DomainException("project_not_found", "Project was not found"));
  }

  public ProjectDetails internal(String clerkId, UUID projectId) {
    authorization.requireProjectMember(clerkId, projectId);
    var project = get(projectId);
    return new ProjectDetails(
        project, authorization.capabilities(clerkId, project.hubId(), projectId));
  }

  @Transactional
  public ProjectDetails update(String clerkId, UUID projectId, ProjectStore.ProjectUpdate update) {
    authorization.requireProjectAdmin(clerkId, projectId);
    var project = projects.update(projectId, update);
    return new ProjectDetails(
        project, authorization.capabilities(clerkId, project.hubId(), projectId));
  }

  @Transactional
  public ProjectStore.ProjectView transition(String clerkId, UUID projectId, ProjectStatus status) {
    authorization.requireProjectAdmin(clerkId, projectId);
    var current = get(projectId);
    var lifecycle = new ProjectLifecycle(current.status());
    lifecycle.transitionTo(status);
    return projects.updateStatus(projectId, current.status(), lifecycle.status());
  }

  @Transactional
  public void assignAdmin(String clerkId, UUID projectId, UUID accountId) {
    var project = get(projectId);
    authorization.requireHubManager(clerkId, project.hubId());
    if (!projects.isActiveHubMember(project.hubId(), accountId)) {
      throw new DomainException(
          "project_admin_must_be_hub_member", "Project administrator must be an active hub member");
    }
    projects.assignAdmin(projectId, accountId);
  }

  private ProjectStore.ProjectView get(UUID projectId) {
    return projects
        .find(projectId)
        .orElseThrow(() -> new DomainException("project_not_found", "Project was not found"));
  }

  public record ProjectDetails(
      ProjectStore.ProjectView project, ViewerCapabilities viewerCapabilities) {}
}
