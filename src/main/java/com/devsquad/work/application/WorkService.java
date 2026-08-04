package com.devsquad.work.application;

import com.devsquad.shared.domain.DomainException;
import com.devsquad.shared.security.AuthorizationService;
import com.devsquad.work.application.port.WorkStore;
import com.devsquad.work.domain.WorkflowGroup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class WorkService {
  private static final Set<String> PRIORITIES = Set.of("NONE", "LOW", "MEDIUM", "HIGH", "URGENT");

  private final WorkStore work;
  private final AuthorizationService authorization;

  public WorkService(WorkStore work, AuthorizationService authorization) {
    this.work = work;
    this.authorization = authorization;
  }

  public BoardView board(String clerkId, UUID projectId) {
    authorization.requireProjectMember(clerkId, projectId);
    return work.board(projectId);
  }

  public TaskView task(String clerkId, UUID taskId) {
    authorization.requireProjectMember(clerkId, work.projectForTask(taskId));
    return work.task(taskId);
  }

  @Transactional
  public TaskView createTask(String clerkId, UUID projectId, TaskCommand command) {
    requirePriority(command.priority());
    var actorId = authorization.requireProjectMember(clerkId, projectId);
    return work.createTask(projectId, actorId, command);
  }

  @Transactional
  public TaskView updateTask(
      String clerkId, UUID taskId, TaskCommand command, int expectedVersion) {
    requirePriority(command.priority());
    var projectId = work.projectForTask(taskId);
    var actorId = authorization.requireProjectMember(clerkId, projectId);
    return work.updateTask(taskId, projectId, actorId, command, expectedVersion);
  }

  @Transactional
  public TaskView move(
      String clerkId, UUID taskId, UUID columnId, int position, int expectedVersion) {
    var projectId = work.projectForTask(taskId);
    var actorId = authorization.requireProjectMember(clerkId, projectId);
    return work.move(taskId, projectId, actorId, columnId, position, expectedVersion);
  }

  @Transactional
  public UUID comment(String clerkId, UUID taskId, String body) {
    if (body == null || body.isBlank()) {
      throw new DomainException("invalid_comment", "Comment cannot be blank");
    }
    var projectId = work.projectForTask(taskId);
    var actorId = authorization.requireProjectMember(clerkId, projectId);
    return work.comment(taskId, projectId, actorId, body.trim());
  }

  public List<CommentView> comments(String clerkId, UUID taskId) {
    authorization.requireProjectMember(clerkId, work.projectForTask(taskId));
    return work.comments(taskId);
  }

  @Transactional
  public UUID createMilestone(String clerkId, UUID projectId, MilestoneCommand command) {
    var actorId = authorization.requireProjectAdmin(clerkId, projectId);
    return work.createMilestone(projectId, actorId, command);
  }

  @Transactional
  public UUID createColumn(
      String clerkId, UUID projectId, String name, String semanticGroup, int position) {
    authorization.requireProjectAdmin(clerkId, projectId);
    try {
      WorkflowGroup.valueOf(semanticGroup);
    } catch (IllegalArgumentException exception) {
      throw new DomainException("invalid_workflow_group", "Workflow semantic group is invalid");
    }
    return work.createColumn(projectId, name.trim(), semanticGroup, position);
  }

  @Transactional
  public UUID createLabel(String clerkId, UUID projectId, String name, String color) {
    authorization.requireProjectAdmin(clerkId, projectId);
    if (!color.matches("#[0-9a-fA-F]{6}")) {
      throw new DomainException("invalid_label_color", "Color must use #RRGGBB");
    }
    return work.createLabel(projectId, name.trim(), color);
  }

  @Transactional
  public void addLabel(String clerkId, UUID taskId, UUID labelId) {
    var projectId = work.projectForTask(taskId);
    authorization.requireProjectMember(clerkId, projectId);
    work.addLabel(taskId, projectId, labelId);
  }

  @Transactional
  public void removeLabel(String clerkId, UUID taskId, UUID labelId) {
    authorization.requireProjectMember(clerkId, work.projectForTask(taskId));
    work.removeLabel(taskId, labelId);
  }

  @Transactional
  public void addAssignee(String clerkId, UUID taskId, UUID accountId) {
    var projectId = work.projectForTask(taskId);
    authorization.requireProjectMember(clerkId, projectId);
    work.addAssignee(taskId, projectId, accountId);
  }

  @Transactional
  public void removeAssignee(String clerkId, UUID taskId, UUID accountId) {
    authorization.requireProjectMember(clerkId, work.projectForTask(taskId));
    work.removeAssignee(taskId, accountId);
  }

  public List<ActivityView> activity(String clerkId, UUID projectId) {
    authorization.requireProjectMember(clerkId, projectId);
    return work.activity(projectId);
  }

  private static void requirePriority(String priority) {
    if (!PRIORITIES.contains(priority)) {
      throw new DomainException("invalid_task_priority", "Task priority is invalid");
    }
  }

  public record TaskCommand(
      UUID parentId,
      UUID columnId,
      UUID milestoneId,
      String title,
      String description,
      String priority,
      LocalDate startDate,
      LocalDate dueDate,
      int position,
      List<UUID> assigneeIds) {}

  public record MilestoneCommand(
      String title, String description, LocalDate startDate, LocalDate dueDate) {}

  public record BoardView(UUID projectId, List<ColumnView> columns) {}

  public record ColumnView(
      UUID id, String name, String semanticGroup, int position, List<TaskView> tasks) {}

  public record TaskView(
      UUID id,
      long sequence,
      UUID parentId,
      UUID columnId,
      UUID milestoneId,
      String title,
      String description,
      String priority,
      LocalDate startDate,
      LocalDate dueDate,
      int position,
      int version,
      OffsetDateTime completedAt,
      List<AssigneeView> assignees) {}

  public record AssigneeView(UUID id, String displayName, String avatarUrl) {}

  public record CommentView(
      UUID id,
      String body,
      OffsetDateTime createdAt,
      UUID authorId,
      String authorName,
      String authorAvatarUrl) {}

  public record ActivityView(
      UUID id,
      String eventType,
      String entityType,
      UUID entityId,
      String actorName,
      OffsetDateTime occurredAt) {}
}
