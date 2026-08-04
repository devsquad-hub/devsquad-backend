package com.devsquad.proposal.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.proposal.application.ProposalService;
import com.devsquad.proposal.application.port.ProposalStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/v1")
public class ProposalController {

  private final ProposalService service;

  public ProposalController(ProposalService service) {
    this.service = service;
  }

  @POST
  @Path("/hubs/{hubId}/proposals")
  public Response create(
      @Context SecurityContext securityContext,
      @PathParam("hubId") UUID hubId,
      @Valid ProposalRequest request) {
    var proposal = service.create(subject(securityContext), hubId, request.toContent());
    return Response.created(URI.create("/api/v1/proposals/" + proposal.id()))
        .entity(proposal)
        .build();
  }

  @GET
  @Path("/hubs/{hubId}/proposals")
  public List<ProposalStore.ProposalView> list(
      @Context SecurityContext securityContext, @PathParam("hubId") UUID hubId) {
    return service.list(subject(securityContext), hubId);
  }

  @PATCH
  @Path("/proposals/{proposalId}")
  public ProposalStore.ProposalView edit(
      @Context SecurityContext securityContext,
      @PathParam("proposalId") UUID proposalId,
      @Valid ProposalRequest request) {
    return service.edit(subject(securityContext), proposalId, request.toContent());
  }

  @POST
  @Path("/proposals/{proposalId}/submit")
  public ProposalStore.ProposalView submit(
      @Context SecurityContext securityContext, @PathParam("proposalId") UUID proposalId) {
    return service.submit(subject(securityContext), proposalId);
  }

  @POST
  @Path("/proposals/{proposalId}/approve")
  public ProposalStore.ProposalView approve(
      @Context SecurityContext securityContext, @PathParam("proposalId") UUID proposalId) {
    return service.approve(subject(securityContext), proposalId);
  }

  @POST
  @Path("/proposals/{proposalId}/reject")
  public ProposalStore.ProposalView reject(
      @Context SecurityContext securityContext,
      @PathParam("proposalId") UUID proposalId,
      @Valid RejectRequest request) {
    return service.reject(subject(securityContext), proposalId, request.reason());
  }

  public record ProposalRequest(
      @NotBlank @Size(max = 180) String title,
      @NotBlank @Size(max = 4_000) String summary,
      @Size(max = 8_000) String problem,
      @Size(max = 8_000) String proposedSolution,
      @Size(max = 8_000) String goals,
      @Size(max = 30) List<@NotBlank @Size(max = 80) String> desiredSkills) {
    ProposalStore.ProposalContent toContent() {
      return new ProposalStore.ProposalContent(
          title, summary, problem, proposedSolution, goals, desiredSkills);
    }
  }

  public record RejectRequest(@NotBlank @Size(max = 2_000) String reason) {}
}
