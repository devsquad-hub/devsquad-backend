package com.devsquad.work.adapter.out.persistence;

import com.devsquad.shared.domain.DomainException;
import com.devsquad.work.application.WorkService.ActivityView;
import com.devsquad.work.application.WorkService.AssigneeView;
import com.devsquad.work.application.WorkService.BoardView;
import com.devsquad.work.application.WorkService.ColumnView;
import com.devsquad.work.application.WorkService.CommentView;
import com.devsquad.work.application.WorkService.MilestoneCommand;
import com.devsquad.work.application.WorkService.TaskCommand;
import com.devsquad.work.application.WorkService.TaskView;
import com.devsquad.work.application.port.WorkStore;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcWorkStore implements WorkStore {
    private final JdbcClient jdbc;

    public JdbcWorkStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public BoardView board(UUID projectId) {
        var columns = jdbc.sql("""
                        select id, name, semantic_group, position from workflow_columns
                        where project_id = :projectId and archived_at is null order by position
                        """).param("projectId", projectId)
                .query((rs, row) -> new ColumnView(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getString("semantic_group"), rs.getInt("position"),
                        tasks(projectId, rs.getObject("id", UUID.class))))
                .list();
        return new BoardView(projectId, columns);
    }

    @Override
    public UUID projectForTask(UUID taskId) {
        return jdbc.sql("select project_id from tasks where id = :id").param("id", taskId)
                .query(UUID.class).optional()
                .orElseThrow(() -> new DomainException("task_not_found", "Task was not found"));
    }

    @Override
    public TaskView task(UUID taskId) {
        return findTask(taskId);
    }

    @Override
    public TaskView createTask(UUID projectId, UUID actorId, TaskCommand command) {
        requireColumn(projectId, command.columnId());
        requireOptionalParent(projectId, command.parentId(), "tasks", "parent_task_not_found");
        requireOptionalParent(projectId, command.milestoneId(), "milestones", "milestone_not_found");
        var sequence = jdbc.sql("""
                        update projects set next_task_number = next_task_number + 1, updated_at = now()
                        where id = :id returning next_task_number - 1
                        """).param("id", projectId).query(Long.class).single();
        var id = UUID.randomUUID();
        jdbc.sql("""
                        insert into tasks (id, project_id, sequence, parent_id, column_id, milestone_id, title,
                                           description, priority, start_date, due_date, position, created_by)
                        values (:id, :projectId, :sequence, :parentId, :columnId, :milestoneId, :title,
                                :description, :priority, :startDate, :dueDate, :position, :actorId)
                        """)
                .param("id", id).param("projectId", projectId).param("sequence", sequence)
                .param("parentId", command.parentId()).param("columnId", command.columnId())
                .param("milestoneId", command.milestoneId()).param("title", command.title().trim())
                .param("description", command.description()).param("priority", command.priority())
                .param("startDate", command.startDate()).param("dueDate", command.dueDate())
                .param("position", command.position()).param("actorId", actorId).update();
        replaceAssignees(id, projectId, command.assigneeIds());
        recordActivity(projectId, actorId, "TASK_CREATED", "TASK", id);
        return findTask(id);
    }

    @Override
    public TaskView updateTask(UUID taskId, UUID projectId, UUID actorId, TaskCommand command, int expectedVersion) {
        requireColumn(projectId, command.columnId());
        requireOptionalParent(projectId, command.parentId(), "tasks", "parent_task_not_found");
        requireOptionalParent(projectId, command.milestoneId(), "milestones", "milestone_not_found");
        var updated = jdbc.sql("""
                        update tasks set parent_id = :parentId, column_id = :columnId, milestone_id = :milestoneId,
                            title = :title, description = :description, priority = :priority,
                            start_date = :startDate, due_date = :dueDate, position = :position,
                            version = version + 1, updated_at = now()
                        where id = :id and version = :version
                        """).param("parentId", command.parentId()).param("columnId", command.columnId())
                .param("milestoneId", command.milestoneId()).param("title", command.title().trim())
                .param("description", command.description()).param("priority", command.priority())
                .param("startDate", command.startDate()).param("dueDate", command.dueDate())
                .param("position", command.position()).param("id", taskId).param("version", expectedVersion).update();
        if (updated == 0) {
            throw new DomainException("stale_task_version", "Task changed since it was loaded");
        }
        jdbc.sql("delete from task_assignees where task_id = :task").param("task", taskId).update();
        replaceAssignees(taskId, projectId, command.assigneeIds());
        recordActivity(projectId, actorId, "TASK_UPDATED", "TASK", taskId);
        return findTask(taskId);
    }

    @Override
    public TaskView move(UUID taskId, UUID projectId, UUID actorId, UUID columnId, int position, int expectedVersion) {
        var group = requireColumn(projectId, columnId);
        var updated = jdbc.sql("""
                        update tasks set column_id = :columnId, position = :position, version = version + 1,
                            updated_at = now(), completed_at = case when :completed then coalesce(completed_at, now()) else null end
                        where id = :id and version = :version
                        """).param("columnId", columnId).param("position", position)
                .param("completed", "COMPLETED".equals(group)).param("id", taskId)
                .param("version", expectedVersion).update();
        if (updated == 0) {
            throw new DomainException("stale_task_version", "Task changed since it was loaded");
        }
        recordActivity(projectId, actorId, "TASK_MOVED", "TASK", taskId);
        return findTask(taskId);
    }

    @Override
    public UUID comment(UUID taskId, UUID projectId, UUID actorId, String body) {
        var id = UUID.randomUUID();
        jdbc.sql("insert into comments (id, task_id, author_id, body) values (:id, :taskId, :authorId, :body)")
                .param("id", id).param("taskId", taskId).param("authorId", actorId).param("body", body).update();
        recordActivity(projectId, actorId, "COMMENT_CREATED", "COMMENT", id);
        return id;
    }

    @Override
    public List<CommentView> comments(UUID taskId) {
        return jdbc.sql("""
                        select c.id, c.body, c.created_at, a.id as author_id, a.display_name, a.avatar_url
                        from comments c join accounts a on a.id = c.author_id
                        where c.task_id = :taskId and c.deleted_at is null order by c.created_at
                        """).param("taskId", taskId)
                .query((rs, row) -> new CommentView(rs.getObject("id", UUID.class), rs.getString("body"),
                        rs.getObject("created_at", OffsetDateTime.class), rs.getObject("author_id", UUID.class),
                        rs.getString("display_name"), rs.getString("avatar_url"))).list();
    }

    @Override
    public UUID createMilestone(UUID projectId, UUID actorId, MilestoneCommand command) {
        var id = UUID.randomUUID();
        jdbc.sql("""
                        insert into milestones (id, project_id, title, description, start_date, due_date, created_by)
                        values (:id, :projectId, :title, :description, :startDate, :dueDate, :actorId)
                        """).param("id", id).param("projectId", projectId).param("title", command.title())
                .param("description", command.description()).param("startDate", command.startDate())
                .param("dueDate", command.dueDate()).param("actorId", actorId).update();
        return id;
    }

    @Override
    public UUID createColumn(UUID projectId, String name, String semanticGroup, int position) {
        var id = UUID.randomUUID();
        jdbc.sql("""
                        insert into workflow_columns (id, project_id, name, semantic_group, position)
                        values (:id, :project, :name, :group, :position)
                        """).param("id", id).param("project", projectId).param("name", name)
                .param("group", semanticGroup).param("position", position).update();
        return id;
    }

    @Override
    public UUID createLabel(UUID projectId, String name, String color) {
        var id = UUID.randomUUID();
        jdbc.sql("insert into labels (id, project_id, name, color) values (:id, :project, :name, :color)")
                .param("id", id).param("project", projectId).param("name", name).param("color", color).update();
        return id;
    }

    @Override
    public void addLabel(UUID taskId, UUID projectId, UUID labelId) {
        var matching = jdbc.sql("select count(*) from labels where id = :label and project_id = :project")
                .param("label", labelId).param("project", projectId).query(Integer.class).single();
        if (matching == 0) {
            throw new DomainException("label_not_found", "Label was not found in this project");
        }
        jdbc.sql("insert into task_labels (task_id, label_id) values (:task, :label) on conflict do nothing")
                .param("task", taskId).param("label", labelId).update();
    }

    @Override
    public void removeLabel(UUID taskId, UUID labelId) {
        jdbc.sql("delete from task_labels where task_id = :task and label_id = :label")
                .param("task", taskId).param("label", labelId).update();
    }

    @Override
    public void addAssignee(UUID taskId, UUID projectId, UUID accountId) {
        requireProjectMember(projectId, accountId);
        jdbc.sql("insert into task_assignees (task_id, account_id) values (:task, :account) on conflict do nothing")
                .param("task", taskId).param("account", accountId).update();
    }

    @Override
    public void removeAssignee(UUID taskId, UUID accountId) {
        jdbc.sql("delete from task_assignees where task_id = :task and account_id = :account")
                .param("task", taskId).param("account", accountId).update();
    }

    @Override
    public List<ActivityView> activity(UUID projectId) {
        return jdbc.sql("""
                        select ae.id, ae.event_type, ae.entity_type, ae.entity_id, ae.occurred_at,
                               a.display_name as actor_name from activity_events ae
                        left join accounts a on a.id = ae.actor_id where ae.project_id = :project
                        order by ae.occurred_at desc limit 200
                        """).param("project", projectId)
                .query((rs, row) -> new ActivityView(rs.getObject("id", UUID.class), rs.getString("event_type"),
                        rs.getString("entity_type"), rs.getObject("entity_id", UUID.class), rs.getString("actor_name"),
                        rs.getObject("occurred_at", OffsetDateTime.class))).list();
    }

    private List<TaskView> tasks(UUID projectId, UUID columnId) {
        return jdbc.sql("""
                        select id, sequence, parent_id, milestone_id, title, description, priority,
                               start_date, due_date, position, version, completed_at
                        from tasks where project_id = :projectId and column_id = :columnId order by position, created_at
                        """).param("projectId", projectId).param("columnId", columnId)
                .query((rs, row) -> new TaskView(rs.getObject("id", UUID.class), rs.getLong("sequence"),
                        rs.getObject("parent_id", UUID.class), columnId, rs.getObject("milestone_id", UUID.class),
                        rs.getString("title"), rs.getString("description"), rs.getString("priority"),
                        rs.getObject("start_date", LocalDate.class), rs.getObject("due_date", LocalDate.class),
                        rs.getInt("position"), rs.getInt("version"), rs.getObject("completed_at", OffsetDateTime.class),
                        assignees(rs.getObject("id", UUID.class)))).list();
    }

    private TaskView findTask(UUID id) {
        return jdbc.sql("""
                        select id, sequence, parent_id, column_id, milestone_id, title, description, priority,
                               start_date, due_date, position, version, completed_at from tasks where id = :id
                        """).param("id", id)
                .query((rs, row) -> new TaskView(id, rs.getLong("sequence"), rs.getObject("parent_id", UUID.class),
                        rs.getObject("column_id", UUID.class), rs.getObject("milestone_id", UUID.class),
                        rs.getString("title"), rs.getString("description"), rs.getString("priority"),
                        rs.getObject("start_date", LocalDate.class), rs.getObject("due_date", LocalDate.class),
                        rs.getInt("position"), rs.getInt("version"), rs.getObject("completed_at", OffsetDateTime.class),
                        assignees(id))).single();
    }

    private List<AssigneeView> assignees(UUID taskId) {
        return jdbc.sql("""
                        select a.id, a.display_name, a.avatar_url from task_assignees ta
                        join accounts a on a.id = ta.account_id where ta.task_id = :taskId order by a.display_name
                        """).param("taskId", taskId)
                .query((rs, row) -> new AssigneeView(rs.getObject("id", UUID.class),
                        rs.getString("display_name"), rs.getString("avatar_url"))).list();
    }

    private void replaceAssignees(UUID taskId, UUID projectId, List<UUID> assigneeIds) {
        if (assigneeIds == null) return;
        for (var accountId : assigneeIds) {
            requireProjectMember(projectId, accountId);
            jdbc.sql("insert into task_assignees (task_id, account_id) values (:task, :account) on conflict do nothing")
                    .param("task", taskId).param("account", accountId).update();
        }
    }

    private void requireProjectMember(UUID projectId, UUID accountId) {
        var active = jdbc.sql("""
                        select count(*) from project_memberships
                        where project_id = :project and account_id = :account and status = 'ACTIVE'
                        """).param("project", projectId).param("account", accountId).query(Integer.class).single();
        if (active == 0) {
            throw new DomainException("assignee_not_project_member", "Every assignee must be a project member");
        }
    }

    private String requireColumn(UUID projectId, UUID columnId) {
        return jdbc.sql("""
                        select semantic_group from workflow_columns
                        where id = :id and project_id = :project and archived_at is null
                        """).param("id", columnId).param("project", projectId).query(String.class).optional()
                .orElseThrow(() -> new DomainException("workflow_column_not_found", "Workflow column was not found"));
    }

    private void requireOptionalParent(UUID projectId, UUID entityId, String table, String code) {
        if (entityId == null) return;
        var sql = "select count(*) from " + table + " where id = :id and project_id = :project";
        if (jdbc.sql(sql).param("id", entityId).param("project", projectId).query(Integer.class).single() == 0) {
            throw new DomainException(code, "Related entity was not found in this project");
        }
    }

    private void recordActivity(UUID projectId, UUID actorId, String event, String entity, UUID entityId) {
        jdbc.sql("""
                        insert into activity_events (hub_id, project_id, actor_id, event_type, entity_type, entity_id)
                        select hub_id, id, :actor, :event, :entity, :entityId from projects where id = :project
                        """).param("actor", actorId).param("event", event).param("entity", entity)
                .param("entityId", entityId).param("project", projectId).update();
    }
}
