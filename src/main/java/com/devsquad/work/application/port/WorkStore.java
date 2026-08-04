package com.devsquad.work.application.port;

import com.devsquad.work.application.WorkService.ActivityView;
import com.devsquad.work.application.WorkService.BoardView;
import com.devsquad.work.application.WorkService.CommentView;
import com.devsquad.work.application.WorkService.MilestoneCommand;
import com.devsquad.work.application.WorkService.TaskCommand;
import com.devsquad.work.application.WorkService.TaskView;
import java.util.List;
import java.util.UUID;

public interface WorkStore {
    BoardView board(UUID projectId);
    UUID projectForTask(UUID taskId);
    TaskView task(UUID taskId);
    TaskView createTask(UUID projectId, UUID actorId, TaskCommand command);
    TaskView updateTask(UUID taskId, UUID projectId, UUID actorId, TaskCommand command, int expectedVersion);
    TaskView move(UUID taskId, UUID projectId, UUID actorId, UUID columnId, int position, int expectedVersion);
    UUID comment(UUID taskId, UUID projectId, UUID actorId, String body);
    List<CommentView> comments(UUID taskId);
    UUID createMilestone(UUID projectId, UUID actorId, MilestoneCommand command);
    UUID createColumn(UUID projectId, String name, String semanticGroup, int position);
    UUID createLabel(UUID projectId, String name, String color);
    void addLabel(UUID taskId, UUID projectId, UUID labelId);
    void removeLabel(UUID taskId, UUID labelId);
    void addAssignee(UUID taskId, UUID projectId, UUID accountId);
    void removeAssignee(UUID taskId, UUID accountId);
    List<ActivityView> activity(UUID projectId);
}
