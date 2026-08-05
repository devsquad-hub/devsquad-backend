package com.devsquad.recruitment.adapter.in.web;

import com.devsquad.recruitment.application.RecruitmentService;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.List;
import java.util.UUID;

/** Public recruitment discovery is isolated from authenticated application commands. */
@Path("/api/v1/public/projects")
@PermitAll
public class PublicRecruitmentController {

  private final RecruitmentService service;

  public PublicRecruitmentController(RecruitmentService service) {
    this.service = service;
  }

  @GET
  @Path("/{projectId}/recruitment-positions")
  public List<RecruitmentService.PositionView> positions(@PathParam("projectId") UUID projectId) {
    return service.publicPositions(projectId);
  }
}
