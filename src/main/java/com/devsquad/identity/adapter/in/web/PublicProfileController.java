package com.devsquad.identity.adapter.in.web;

import com.devsquad.identity.application.PublicProfileService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/profiles")
public class PublicProfileController {
    private final PublicProfileService service;

    public PublicProfileController(PublicProfileService service) { this.service = service; }

    @GetMapping("/{accountId}")
    public PublicProfileService.PublicProfile profile(@PathVariable UUID accountId) {
        return service.find(accountId);
    }
}
