package com.devsquad.project.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.project.application.ProjectService;
import com.devsquad.project.application.port.ProjectStore;
import com.devsquad.project.domain.ProjectStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) { this.service = service; }

    @GetMapping("/public/hubs/{hubId}/projects")
    public List<PublicProjectView> publicProjects(@PathVariable UUID hubId) {
        return service.publicProjects(hubId).stream().map(PublicProjectView::from).toList();
    }

    @GetMapping("/public/hubs/{hubSlug}/projects/{projectSlug}")
    public PublicProjectView publicProject(@PathVariable String hubSlug, @PathVariable String projectSlug) {
        return PublicProjectView.from(service.publicProject(hubSlug, projectSlug));
    }

    @GetMapping("/projects/{projectId}")
    public InternalProjectView project(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return InternalProjectView.from(service.internal(subject(jwt), projectId));
    }

    @PatchMapping("/projects/{projectId}")
    public InternalProjectView update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        return InternalProjectView.from(service.update(subject(jwt), projectId, request.toUpdate()));
    }

    @PostMapping("/projects/{projectId}/transition")
    public ProjectStore.ProjectView transition(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId, @Valid @RequestBody TransitionRequest request) {
        return service.transition(subject(jwt), projectId, request.status());
    }

    @PutMapping("/projects/{projectId}/admins/{accountId}")
    public ResponseEntity<Void> assignAdmin(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId, @PathVariable UUID accountId) {
        service.assignAdmin(subject(jwt), projectId, accountId);
        return ResponseEntity.noContent().build();
    }

    public record TransitionRequest(@NotNull ProjectStatus status) {}

    public record UpdateProjectRequest(
            @NotBlank @Size(max = 180) String name,
            @NotBlank @Size(max = 4_000) String summary,
            @Size(max = 20_000) String description,
            @Size(max = 2_000) String repositoryUrl,
            @Size(max = 2_000) String communicationUrl,
            @Size(max = 30) List<@NotBlank @Size(max = 80) String> tags) {
        ProjectStore.ProjectUpdate toUpdate() {
            return new ProjectStore.ProjectUpdate(
                    name, summary, description, repositoryUrl, communicationUrl,
                    tags == null ? List.of() : List.copyOf(tags));
        }
    }

    public record InternalProjectView(
            UUID id, UUID hubId, String name, String slug, String projectKey, String summary,
            String description, ProjectStatus status, String repositoryUrl, String communicationUrl,
            List<String> tags, long totalTasks, long completedTasks, List<ProjectStore.MemberView> members,
            com.devsquad.shared.security.ViewerCapabilities viewerCapabilities) {
        static InternalProjectView from(ProjectService.ProjectDetails details) {
            var project = details.project();
            return new InternalProjectView(
                    project.id(), project.hubId(), project.name(), project.slug(), project.projectKey(),
                    project.summary(), project.description(), project.status(), project.repositoryUrl(),
                    project.communicationUrl(), project.tags(), project.totalTasks(), project.completedTasks(),
                    project.members(), details.viewerCapabilities());
        }
    }

    public record PublicProjectView(UUID id, UUID hubId, String name, String slug, String projectKey, String summary,
                                    String description, ProjectStatus status, String repositoryUrl, List<String> tags,
                                    long totalTasks, long completedTasks, List<ProjectStore.MemberView> members) {
        static PublicProjectView from(ProjectStore.ProjectView project) {
            return new PublicProjectView(project.id(), project.hubId(), project.name(), project.slug(), project.projectKey(),
                    project.summary(), project.description(), project.status(), project.repositoryUrl(), project.tags(),
                    project.totalTasks(), project.completedTasks(), project.members());
        }
    }
}
