package com.devsquad.work.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.work.application.WorkService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class WorkController {

    private final WorkService service;

    public WorkController(WorkService service) { this.service = service; }

    @GetMapping("/projects/{projectId}/board")
    public WorkService.BoardView board(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return service.board(subject(jwt), projectId);
    }

    @GetMapping("/tasks/{taskId}")
    public WorkService.TaskView task(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId) {
        return service.task(subject(jwt), taskId);
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<WorkService.TaskView> createTask(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId, @Valid @RequestBody TaskRequest request) {
        var task = service.createTask(subject(jwt), projectId, request.toCommand());
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + task.id())).body(task);
    }

    @PatchMapping("/tasks/{taskId}")
    public WorkService.TaskView updateTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return service.updateTask(subject(jwt), taskId, request.task().toCommand(), request.expectedVersion());
    }

    @PostMapping("/tasks/{taskId}/move")
    public WorkService.TaskView move(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId, @Valid @RequestBody MoveRequest request) {
        return service.move(subject(jwt), taskId, request.columnId(), request.position(), request.expectedVersion());
    }

    @GetMapping("/tasks/{taskId}/comments")
    public List<WorkService.CommentView> comments(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId) {
        return service.comments(subject(jwt), taskId);
    }

    @PostMapping("/tasks/{taskId}/comments")
    public ResponseEntity<IdResponse> comment(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId, @Valid @RequestBody CommentRequest request) {
        var id = service.comment(subject(jwt), taskId, request.body());
        return ResponseEntity.created(URI.create("/api/v1/comments/" + id)).body(new IdResponse(id));
    }

    @PostMapping("/projects/{projectId}/milestones")
    public ResponseEntity<IdResponse> milestone(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId, @Valid @RequestBody MilestoneRequest request) {
        var id = service.createMilestone(subject(jwt), projectId,
                new WorkService.MilestoneCommand(request.title(), request.description(), request.startDate(), request.dueDate()));
        return ResponseEntity.created(URI.create("/api/v1/milestones/" + id)).body(new IdResponse(id));
    }

    @PostMapping("/projects/{projectId}/workflow-columns")
    public ResponseEntity<IdResponse> column(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId, @Valid @RequestBody ColumnRequest request) {
        var id = service.createColumn(subject(jwt), projectId, request.name(), request.semanticGroup(), request.position());
        return ResponseEntity.created(URI.create("/api/v1/workflow-columns/" + id)).body(new IdResponse(id));
    }

    @PostMapping("/projects/{projectId}/labels")
    public ResponseEntity<IdResponse> label(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId, @Valid @RequestBody LabelRequest request) {
        var id = service.createLabel(subject(jwt), projectId, request.name(), request.color());
        return ResponseEntity.created(URI.create("/api/v1/labels/" + id)).body(new IdResponse(id));
    }

    @PostMapping("/tasks/{taskId}/labels/{labelId}")
    public ResponseEntity<Void> addLabel(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId, @PathVariable UUID labelId) {
        service.addLabel(subject(jwt), taskId, labelId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/tasks/{taskId}/labels/{labelId}")
    public ResponseEntity<Void> removeLabel(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId, @PathVariable UUID labelId) {
        service.removeLabel(subject(jwt), taskId, labelId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/tasks/{taskId}/assignees/{accountId}")
    public ResponseEntity<Void> addAssignee(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId, @PathVariable UUID accountId) {
        service.addAssignee(subject(jwt), taskId, accountId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/tasks/{taskId}/assignees/{accountId}")
    public ResponseEntity<Void> removeAssignee(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId, @PathVariable UUID accountId) {
        service.removeAssignee(subject(jwt), taskId, accountId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects/{projectId}/activity")
    public List<WorkService.ActivityView> activity(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return service.activity(subject(jwt), projectId);
    }

    public record TaskRequest(UUID parentId, @NotNull UUID columnId, UUID milestoneId,
                              @NotBlank @Size(max = 240) String title, @Size(max = 20_000) String description,
                              @NotBlank String priority, LocalDate startDate, LocalDate dueDate, int position,
                              List<UUID> assigneeIds) {
        WorkService.TaskCommand toCommand() {
            return new WorkService.TaskCommand(parentId, columnId, milestoneId, title, description, priority,
                    startDate, dueDate, position, assigneeIds);
        }
    }
    public record MoveRequest(@NotNull UUID columnId, int position, int expectedVersion) {}
    public record UpdateTaskRequest(@NotNull @Valid TaskRequest task, int expectedVersion) {}
    public record CommentRequest(@NotBlank @Size(max = 10_000) String body) {}
    public record MilestoneRequest(@NotBlank @Size(max = 180) String title, @Size(max = 4_000) String description,
                                   LocalDate startDate, LocalDate dueDate) {}
    public record ColumnRequest(@NotBlank @Size(max = 100) String name, @NotBlank String semanticGroup, int position) {}
    public record LabelRequest(@NotBlank @Size(max = 80) String name, @NotBlank String color) {}
    public record IdResponse(UUID id) {}
}
