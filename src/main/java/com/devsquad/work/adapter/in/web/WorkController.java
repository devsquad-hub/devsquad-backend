package com.devsquad.work.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.work.application.WorkService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Path("/api/v1")
public class WorkController {

  private final WorkService service;

  public WorkController(WorkService service) {
    this.service = service;
  }

  @GET
  @Path("/projects/{projectId}/board")
  public WorkService.BoardView board(
      @Context SecurityContext securityContext, @PathParam("projectId") UUID projectId) {
    return service.board(subject(securityContext), projectId);
  }

  @GET
  @Path("/tasks/{taskId}")
  public WorkService.TaskView task(
      @Context SecurityContext securityContext, @PathParam("taskId") UUID taskId) {
    return service.task(subject(securityContext), taskId);
  }

  @POST
  @Path("/projects/{projectId}/tasks")
  public Response createTask(
      @Context SecurityContext securityContext,
      @PathParam("projectId") UUID projectId,
      @Valid TaskRequest request) {
    var task = service.createTask(subject(securityContext), projectId, request.toCommand());
    return Response.created(URI.create("/api/v1/tasks/" + task.id())).entity(task).build();
  }

  @PATCH
  @Path("/tasks/{taskId}")
  public WorkService.TaskView updateTask(
      @Context SecurityContext securityContext,
      @PathParam("taskId") UUID taskId,
      @Valid UpdateTaskRequest request) {
    return service.updateTask(
        subject(securityContext), taskId, request.task().toCommand(), request.expectedVersion());
  }

  @POST
  @Path("/tasks/{taskId}/move")
  public WorkService.TaskView move(
      @Context SecurityContext securityContext,
      @PathParam("taskId") UUID taskId,
      @Valid MoveRequest request) {
    return service.move(
        subject(securityContext),
        taskId,
        request.columnId(),
        request.position(),
        request.expectedVersion());
  }

  @GET
  @Path("/tasks/{taskId}/comments")
  public List<WorkService.CommentView> comments(
      @Context SecurityContext securityContext, @PathParam("taskId") UUID taskId) {
    return service.comments(subject(securityContext), taskId);
  }

  @POST
  @Path("/tasks/{taskId}/comments")
  public Response comment(
      @Context SecurityContext securityContext,
      @PathParam("taskId") UUID taskId,
      @Valid CommentRequest request) {
    var id = service.comment(subject(securityContext), taskId, request.body());
    return Response.created(URI.create("/api/v1/comments/" + id))
        .entity(new IdResponse(id))
        .build();
  }

  @POST
  @Path("/projects/{projectId}/milestones")
  public Response milestone(
      @Context SecurityContext securityContext,
      @PathParam("projectId") UUID projectId,
      @Valid MilestoneRequest request) {
    var id =
        service.createMilestone(
            subject(securityContext),
            projectId,
            new WorkService.MilestoneCommand(
                request.title(), request.description(), request.startDate(), request.dueDate()));
    return Response.created(URI.create("/api/v1/milestones/" + id))
        .entity(new IdResponse(id))
        .build();
  }

  @POST
  @Path("/projects/{projectId}/workflow-columns")
  public Response column(
      @Context SecurityContext securityContext,
      @PathParam("projectId") UUID projectId,
      @Valid ColumnRequest request) {
    var id =
        service.createColumn(
            subject(securityContext),
            projectId,
            request.name(),
            request.semanticGroup(),
            request.position());
    return Response.created(URI.create("/api/v1/workflow-columns/" + id))
        .entity(new IdResponse(id))
        .build();
  }

  @POST
  @Path("/projects/{projectId}/labels")
  public Response label(
      @Context SecurityContext securityContext,
      @PathParam("projectId") UUID projectId,
      @Valid LabelRequest request) {
    var id =
        service.createLabel(subject(securityContext), projectId, request.name(), request.color());
    return Response.created(URI.create("/api/v1/labels/" + id)).entity(new IdResponse(id)).build();
  }

  @POST
  @Path("/tasks/{taskId}/labels/{labelId}")
  public Response addLabel(
      @Context SecurityContext securityContext,
      @PathParam("taskId") UUID taskId,
      @PathParam("labelId") UUID labelId) {
    service.addLabel(subject(securityContext), taskId, labelId);
    return Response.noContent().build();
  }

  @DELETE
  @Path("/tasks/{taskId}/labels/{labelId}")
  public Response removeLabel(
      @Context SecurityContext securityContext,
      @PathParam("taskId") UUID taskId,
      @PathParam("labelId") UUID labelId) {
    service.removeLabel(subject(securityContext), taskId, labelId);
    return Response.noContent().build();
  }

  @PUT
  @Path("/tasks/{taskId}/assignees/{accountId}")
  public Response addAssignee(
      @Context SecurityContext securityContext,
      @PathParam("taskId") UUID taskId,
      @PathParam("accountId") UUID accountId) {
    service.addAssignee(subject(securityContext), taskId, accountId);
    return Response.noContent().build();
  }

  @DELETE
  @Path("/tasks/{taskId}/assignees/{accountId}")
  public Response removeAssignee(
      @Context SecurityContext securityContext,
      @PathParam("taskId") UUID taskId,
      @PathParam("accountId") UUID accountId) {
    service.removeAssignee(subject(securityContext), taskId, accountId);
    return Response.noContent().build();
  }

  @GET
  @Path("/projects/{projectId}/activity")
  public List<WorkService.ActivityView> activity(
      @Context SecurityContext securityContext, @PathParam("projectId") UUID projectId) {
    return service.activity(subject(securityContext), projectId);
  }

  public record TaskRequest(
      UUID parentId,
      @NotNull UUID columnId,
      UUID milestoneId,
      @NotBlank @Size(max = 240) String title,
      @Size(max = 20_000) String description,
      @NotBlank String priority,
      LocalDate startDate,
      LocalDate dueDate,
      int position,
      List<UUID> assigneeIds) {
    WorkService.TaskCommand toCommand() {
      return new WorkService.TaskCommand(
          parentId,
          columnId,
          milestoneId,
          title,
          description,
          priority,
          startDate,
          dueDate,
          position,
          assigneeIds);
    }
  }

  public record MoveRequest(@NotNull UUID columnId, int position, int expectedVersion) {}

  public record UpdateTaskRequest(@NotNull @Valid TaskRequest task, int expectedVersion) {}

  public record CommentRequest(@NotBlank @Size(max = 10_000) String body) {}

  public record MilestoneRequest(
      @NotBlank @Size(max = 180) String title,
      @Size(max = 4_000) String description,
      LocalDate startDate,
      LocalDate dueDate) {}

  public record ColumnRequest(
      @NotBlank @Size(max = 100) String name, @NotBlank String semanticGroup, int position) {}

  public record LabelRequest(@NotBlank @Size(max = 80) String name, @NotBlank String color) {}

  public record IdResponse(UUID id) {}
}
