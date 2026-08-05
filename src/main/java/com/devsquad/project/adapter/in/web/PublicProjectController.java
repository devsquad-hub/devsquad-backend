package com.devsquad.project.adapter.in.web;

import com.devsquad.project.application.ProjectService;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.List;
import java.util.UUID;

/** Public project catalog endpoints are isolated from authenticated project commands. */
@Path("/api/v1/public/hubs")
@PermitAll
public class PublicProjectController {

  private final ProjectService service;

  public PublicProjectController(ProjectService service) {
    this.service = service;
  }

  @GET
  @Path("/{hubId}/projects")
  public List<ProjectController.PublicProjectView> publicProjects(
      @PathParam("hubId") UUID hubId) {
    return service.publicProjects(hubId).stream()
        .map(ProjectController.PublicProjectView::from)
        .toList();
  }

  @GET
  @Path("/{hubSlug}/projects/{projectSlug}")
  public ProjectController.PublicProjectView publicProject(
      @PathParam("hubSlug") String hubSlug, @PathParam("projectSlug") String projectSlug) {
    return ProjectController.PublicProjectView.from(service.publicProject(hubSlug, projectSlug));
  }
}
