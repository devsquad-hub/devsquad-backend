package com.devsquad.hub.adapter.in.web;

import com.devsquad.hub.application.HubQueryService;
import com.devsquad.hub.domain.Hub;
import com.devsquad.shared.web.PageResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/hubs")
class PublicHubController {

    private final HubQueryService service;

    PublicHubController(HubQueryService service) {
        this.service = service;
    }

    @GetMapping
    PageResponse<HubResponse> findAll() {
        var hubs = service.findAll().stream().map(HubResponse::from).toList();
        return PageResponse.singlePage(hubs);
    }

    record HubResponse(UUID id, String name, String slug, String description) {
        static HubResponse from(Hub hub) {
            return new HubResponse(hub.id(), hub.name(), hub.slug(), hub.description());
        }
    }
}
