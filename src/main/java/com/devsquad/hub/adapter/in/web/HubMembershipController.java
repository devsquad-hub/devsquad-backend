package com.devsquad.hub.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.hub.application.HubMembershipService;
import com.devsquad.hub.application.port.HubMembershipStore;
import com.devsquad.hub.domain.HubRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.UUID;

@Path("/api/v1")
public class HubMembershipController {

  private final HubMembershipService service;

  public HubMembershipController(HubMembershipService service) {
    this.service = service;
  }

  @GET
  @Path("/hubs")
  public List<HubMembershipStore.MembershipView> mine(@Context SecurityContext securityContext) {
    return service.mine(subject(securityContext));
  }

  @GET
  @Path("/hubs/{hubId}/members")
  public List<HubMembershipStore.MemberView> members(
      @Context SecurityContext securityContext, @PathParam("hubId") UUID hubId) {
    return service.members(subject(securityContext), hubId);
  }

  @PUT
  @Path("/hubs/{hubId}/members/{accountId}")
  public Response assign(
      @Context SecurityContext securityContext,
      @PathParam("hubId") UUID hubId,
      @PathParam("accountId") UUID accountId,
      @Valid AssignRoleRequest request) {
    service.assign(subject(securityContext), hubId, accountId, request.role());
    return Response.noContent().build();
  }

  public record AssignRoleRequest(@NotNull HubRole role) {}
}
