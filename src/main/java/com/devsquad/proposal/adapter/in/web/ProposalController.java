package com.devsquad.proposal.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.proposal.application.ProposalService;
import com.devsquad.proposal.application.port.ProposalStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ProposalController {

    private final ProposalService service;

    public ProposalController(ProposalService service) {
        this.service = service;
    }

    @PostMapping("/hubs/{hubId}/proposals")
    public ResponseEntity<ProposalStore.ProposalView> create(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID hubId, @Valid @RequestBody ProposalRequest request) {
        var proposal = service.create(subject(jwt), hubId, request.toContent());
        return ResponseEntity.created(URI.create("/api/v1/proposals/" + proposal.id())).body(proposal);
    }

    @GetMapping("/hubs/{hubId}/proposals")
    public List<ProposalStore.ProposalView> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID hubId) {
        return service.list(subject(jwt), hubId);
    }

    @PatchMapping("/proposals/{proposalId}")
    public ProposalStore.ProposalView edit(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID proposalId, @Valid @RequestBody ProposalRequest request) {
        return service.edit(subject(jwt), proposalId, request.toContent());
    }

    @PostMapping("/proposals/{proposalId}/submit")
    public ProposalStore.ProposalView submit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID proposalId) {
        return service.submit(subject(jwt), proposalId);
    }

    @PostMapping("/proposals/{proposalId}/approve")
    public ProposalStore.ProposalView approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID proposalId) {
        return service.approve(subject(jwt), proposalId);
    }

    @PostMapping("/proposals/{proposalId}/reject")
    public ProposalStore.ProposalView reject(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID proposalId, @Valid @RequestBody RejectRequest request) {
        return service.reject(subject(jwt), proposalId, request.reason());
    }

    public record ProposalRequest(
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 4_000) String summary,
            @Size(max = 8_000) String problem,
            @Size(max = 8_000) String proposedSolution,
            @Size(max = 8_000) String goals,
            @Size(max = 30) List<@NotBlank @Size(max = 80) String> desiredSkills) {
        ProposalStore.ProposalContent toContent() {
            return new ProposalStore.ProposalContent(title, summary, problem, proposedSolution, goals, desiredSkills);
        }
    }

    public record RejectRequest(@NotBlank @Size(max = 2_000) String reason) {}
}
