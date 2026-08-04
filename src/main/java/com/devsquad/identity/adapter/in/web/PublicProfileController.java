package com.devsquad.identity.adapter.in.web;

import com.devsquad.identity.application.PublicProfileService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.UUID;

@Path("/api/v1/public/profiles")
public class PublicProfileController {
  private final PublicProfileService service;

  public PublicProfileController(PublicProfileService service) {
    this.service = service;
  }

  @GET
  @Path("/{accountId}")
  public PublicProfileService.PublicProfile profile(@PathParam("accountId") UUID accountId) {
    return service.find(accountId);
  }
}
