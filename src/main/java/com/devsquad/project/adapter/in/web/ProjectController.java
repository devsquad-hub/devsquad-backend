package com.devsquad.project.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.project.application.ProjectService;
import com.devsquad.project.application.port.ProjectStore;
import com.devsquad.project.domain.ProjectStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.UUID;

@Path("/api/v1")
public class ProjectController {

  private final ProjectService service;

  public ProjectController(ProjectService service) {
    this.service = service;
  }

  @GET
  @Path("/public/hubs/{hubId}/projects")
  public List<PublicProjectView> publicProjects(@PathParam("hubId") UUID hubId) {
    return service.publicProjects(hubId).stream().map(PublicProjectView::from).toList();
  }

  @GET
  @Path("/public/hubs/{hubSlug}/projects/{projectSlug}")
  public PublicProjectView publicProject(
      @PathParam("hubSlug") String hubSlug, @PathParam("projectSlug") String projectSlug) {
    return PublicProjectView.from(service.publicProject(hubSlug, projectSlug));
  }

  @GET
  @Path("/projects/{projectId}")
  public InternalProjectView project(
      @Context SecurityContext securityContext, @PathParam("projectId") UUID projectId) {
    return InternalProjectView.from(service.internal(subject(securityContext), projectId));
  }

  @PATCH
  @Path("/projects/{projectId}")
  public InternalProjectView update(
      @Context SecurityContext securityContext,
      @PathParam("projectId") UUID projectId,
      @Valid UpdateProjectRequest request) {
    return InternalProjectView.from(
        service.update(subject(securityContext), projectId, request.toUpdate()));
  }

  @POST
  @Path("/projects/{projectId}/transition")
  public ProjectStore.ProjectView transition(
      @Context SecurityContext securityContext,
      @PathParam("projectId") UUID projectId,
      @Valid TransitionRequest request) {
    return service.transition(subject(securityContext), projectId, request.status());
  }

  @PUT
  @Path("/projects/{projectId}/admins/{accountId}")
  public Response assignAdmin(
      @Context SecurityContext securityContext,
      @PathParam("projectId") UUID projectId,
      @PathParam("accountId") UUID accountId) {
    service.assignAdmin(subject(securityContext), projectId, accountId);
    return Response.noContent().build();
  }

  public record TransitionRequest(@NotNull ProjectStatus status) {}

  public record UpdateProjectRequest(
      @NotBlank @Size(max = 180) String name,
      @NotBlank @Size(max = 4_000) String summary,
      @Size(max = 20_000) String description,
      @Size(max = 2_000) String repositoryUrl,
      @Size(max = 2_000) String communicationUrl,
      @Size(max = 30) List<@NotBlank @Size(max = 80) String> tags) {
    ProjectStore.ProjectUpdate toUpdate() {
      return new ProjectStore.ProjectUpdate(
          name,
          summary,
          description,
          repositoryUrl,
          communicationUrl,
          tags == null ? List.of() : List.copyOf(tags));
    }
  }

  public record InternalProjectView(
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
      List<ProjectStore.MemberView> members,
      com.devsquad.shared.security.ViewerCapabilities viewerCapabilities) {
    static InternalProjectView from(ProjectService.ProjectDetails details) {
      var project = details.project();
      return new InternalProjectView(
          project.id(),
          project.hubId(),
          project.name(),
          project.slug(),
          project.projectKey(),
          project.summary(),
          project.description(),
          project.status(),
          project.repositoryUrl(),
          project.communicationUrl(),
          project.tags(),
          project.totalTasks(),
          project.completedTasks(),
          project.members(),
          details.viewerCapabilities());
    }
  }

  public record PublicProjectView(
      UUID id,
      UUID hubId,
      String name,
      String slug,
      String projectKey,
      String summary,
      String description,
      ProjectStatus status,
      String repositoryUrl,
      List<String> tags,
      long totalTasks,
      long completedTasks,
      List<ProjectStore.MemberView> members) {
    static PublicProjectView from(ProjectStore.ProjectView project) {
      return new PublicProjectView(
          project.id(),
          project.hubId(),
          project.name(),
          project.slug(),
          project.projectKey(),
          project.summary(),
          project.description(),
          project.status(),
          project.repositoryUrl(),
          project.tags(),
          project.totalTasks(),
          project.completedTasks(),
          project.members());
    }
  }
}
