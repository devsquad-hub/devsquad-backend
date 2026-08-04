package com.devsquad.hub.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.hub.application.HubMembershipService;
import com.devsquad.hub.application.port.HubMembershipStore;
import com.devsquad.hub.domain.HubRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HubMembershipController {

    private final HubMembershipService service;

    public HubMembershipController(HubMembershipService service) {
        this.service = service;
    }

    @GetMapping("/hubs")
    public List<HubMembershipStore.MembershipView> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(subject(jwt));
    }

    @GetMapping("/hubs/{hubId}/members")
    public List<HubMembershipStore.MemberView> members(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID hubId) {
        return service.members(subject(jwt), hubId);
    }

    @PutMapping("/hubs/{hubId}/members/{accountId}")
    public ResponseEntity<Void> assign(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID hubId, @PathVariable UUID accountId,
            @Valid @RequestBody AssignRoleRequest request) {
        service.assign(subject(jwt), hubId, accountId, request.role());
        return ResponseEntity.noContent().build();
    }

    public record AssignRoleRequest(@NotNull HubRole role) {}
}
