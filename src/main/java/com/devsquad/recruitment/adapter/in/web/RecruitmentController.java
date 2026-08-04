package com.devsquad.recruitment.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.recruitment.application.RecruitmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RecruitmentController {

    private final RecruitmentService service;

    public RecruitmentController(RecruitmentService service) { this.service = service; }

    @PostMapping("/projects/{projectId}/recruitment-rounds")
    public ResponseEntity<IdResponse> createRound(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId, @Valid @RequestBody RoundRequest request) {
        var id = service.createRound(subject(jwt), projectId,
                new RecruitmentService.RoundCommand(request.name(), request.description(), request.opensAt(), request.closesAt()));
        return ResponseEntity.created(URI.create("/api/v1/recruitment-rounds/" + id)).body(new IdResponse(id));
    }

    @PostMapping("/recruitment-rounds/{roundId}/positions")
    public ResponseEntity<IdResponse> createPosition(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID roundId, @Valid @RequestBody PositionRequest request) {
        var questions = request.questions() == null ? List.<RecruitmentService.Question>of() : request.questions().stream()
                .map(question -> new RecruitmentService.Question(question.key(), question.label(), question.type(),
                        question.required(), question.options())).toList();
        var id = service.createPosition(subject(jwt), roundId,
                new RecruitmentService.PositionCommand(request.title(), request.description(), request.skills(), request.capacity(), questions));
        return ResponseEntity.created(URI.create("/api/v1/recruitment-positions/" + id)).body(new IdResponse(id));
    }

    @PostMapping("/recruitment-rounds/{roundId}/open")
    public ResponseEntity<Void> open(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID roundId) {
        service.openRound(subject(jwt), roundId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/projects/{projectId}/recruitment-positions")
    public List<RecruitmentService.PositionView> positions(@PathVariable UUID projectId) {
        return service.publicPositions(projectId);
    }

    @PostMapping("/recruitment-positions/{positionId}/applications")
    public ResponseEntity<IdResponse> apply(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID positionId, @RequestBody Map<String, Object> answers) {
        var id = service.apply(subject(jwt), positionId, answers);
        return ResponseEntity.created(URI.create("/api/v1/applications/" + id)).body(new IdResponse(id));
    }

    @PostMapping("/applications/{applicationId}/accept")
    public ResponseEntity<Void> accept(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId, @RequestBody(required = false) DecisionRequest request) {
        service.decide(subject(jwt), applicationId, true, request == null ? null : request.note());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/applications/{applicationId}/reject")
    public ResponseEntity<Void> reject(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId, @RequestBody(required = false) DecisionRequest request) {
        service.decide(subject(jwt), applicationId, false, request == null ? null : request.note());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects/{projectId}/applications")
    public List<RecruitmentService.ApplicationView> applications(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return service.applications(subject(jwt), projectId);
    }

    @PostMapping("/projects/{projectId}/invitations")
    public ResponseEntity<IdResponse> invite(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId, @Valid @RequestBody InviteRequest request) {
        var id = service.invite(subject(jwt), projectId, request.accountId(), request.positionId(), request.functionalRole());
        return ResponseEntity.created(URI.create("/api/v1/invitations/" + id)).body(new IdResponse(id));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<Void> acceptInvitation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invitationId) {
        service.respondToInvitation(subject(jwt), invitationId, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitations/{invitationId}/decline")
    public ResponseEntity<Void> declineInvitation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invitationId) {
        service.respondToInvitation(subject(jwt), invitationId, false);
        return ResponseEntity.noContent().build();
    }

    public record RoundRequest(@NotBlank @Size(max = 160) String name, @Size(max = 4_000) String description,
                               Instant opensAt, Instant closesAt) {}
    public record PositionRequest(@NotBlank @Size(max = 140) String title, @Size(max = 4_000) String description,
                                  @Size(max = 30) List<String> skills, @Min(1) int capacity,
                                  @Size(max = 30) List<@Valid QuestionRequest> questions) {}
    public record QuestionRequest(@NotBlank @Size(max = 64) String key, @NotBlank @Size(max = 500) String label,
                                  @NotBlank String type, boolean required, List<String> options) {}
    public record DecisionRequest(@Size(max = 2_000) String note) {}
    public record InviteRequest(@NotNull UUID accountId, UUID positionId, @Size(max = 120) String functionalRole) {}
    public record IdResponse(UUID id) {}
}
