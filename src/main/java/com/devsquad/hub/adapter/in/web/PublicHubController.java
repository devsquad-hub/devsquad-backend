package com.devsquad.hub.adapter.in.web;

import com.devsquad.hub.application.HubQueryService;
import com.devsquad.hub.domain.Hub;
import com.devsquad.shared.web.PageResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.UUID;

@Path("/api/v1/public/hubs")
public class PublicHubController {

  private final HubQueryService service;

  public PublicHubController(HubQueryService service) {
    this.service = service;
  }

  @GET
  public PageResponse<HubResponse> findAll() {
    var hubs = service.findAll().stream().map(HubResponse::from).toList();
    return PageResponse.singlePage(hubs);
  }

  public record HubResponse(UUID id, String name, String slug, String description) {
    static HubResponse from(Hub hub) {
      return new HubResponse(hub.id(), hub.name(), hub.slug(), hub.description());
    }
  }
}
