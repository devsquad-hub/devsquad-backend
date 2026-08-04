package com.devsquad.identity.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.identity.application.PersonalWorkspaceService;
import com.devsquad.identity.application.port.PersonalWorkspaceStore.ApplicationSummary;
import com.devsquad.identity.application.port.PersonalWorkspaceStore.InvitationSummary;
import com.devsquad.project.application.port.ProjectStore.ProjectView;
import com.devsquad.shared.web.PageResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@Path("/api/v1/me")
public class PersonalWorkspaceController {
  private final PersonalWorkspaceService service;

  public PersonalWorkspaceController(PersonalWorkspaceService service) {
    this.service = service;
  }

  @GET
  @Path("/projects")
  public PageResponse<ProjectView> projects(@Context SecurityContext securityContext) {
    return PageResponse.singlePage(service.projects(subject(securityContext)));
  }

  @GET
  @Path("/applications")
  public PageResponse<ApplicationSummary> applications(@Context SecurityContext securityContext) {
    return PageResponse.singlePage(service.applications(subject(securityContext)));
  }

  @GET
  @Path("/invitations")
  public PageResponse<InvitationSummary> invitations(@Context SecurityContext securityContext) {
    return PageResponse.singlePage(service.invitations(subject(securityContext)));
  }
}
