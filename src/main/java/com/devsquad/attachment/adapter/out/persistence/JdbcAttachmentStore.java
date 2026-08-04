package com.devsquad.attachment.adapter.out.persistence;

import com.devsquad.attachment.application.AttachmentService.UploadCommand;
import com.devsquad.attachment.application.port.AttachmentStore;
import com.devsquad.shared.domain.DomainException;
import com.devsquad.shared.persistence.JdbcClient;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class JdbcAttachmentStore implements AttachmentStore {
  private final JdbcClient jdbc;

  public JdbcAttachmentStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean parentBelongsToProject(UploadCommand command) {
    var count =
        command.taskId() != null
            ? jdbc.sql("select count(*) from tasks where id = :id and project_id = :project")
                .param("id", command.taskId())
                .param("project", command.projectId())
                .query(Integer.class)
                .single()
            : jdbc.sql(
                    """
                    select count(*) from comments c join tasks t on t.id = c.task_id
                    where c.id = :id and t.project_id = :project and c.deleted_at is null
                    """)
                .param("id", command.commentId())
                .param("project", command.projectId())
                .query(Integer.class)
                .single();
    return count > 0;
  }

  @Override
  public void create(UUID id, UUID actorId, String objectKey, UploadCommand command) {
    jdbc.sql(
            """
            insert into attachments (id, project_id, task_id, comment_id, uploaded_by, object_key,
                                     original_name, content_type, size_bytes, status)
            values (:id, :project, :task, :comment, :actor, :key, :name, :type, :size, 'PENDING')
            """)
        .param("id", id)
        .param("project", command.projectId())
        .param("task", command.taskId())
        .param("comment", command.commentId())
        .param("actor", actorId)
        .param("key", objectKey)
        .param("name", command.fileName())
        .param("type", command.contentType())
        .param("size", command.sizeBytes())
        .update();
  }

  @Override
  public StoredAttachment pending(UUID attachmentId) {
    return find(attachmentId, "PENDING", "attachment_not_pending", "Attachment is not pending");
  }

  @Override
  public StoredAttachment ready(UUID attachmentId) {
    return find(attachmentId, "READY", "attachment_not_found", "Attachment was not found");
  }

  @Override
  public void markReady(UUID attachmentId) {
    jdbc.sql("update attachments set status = 'READY', ready_at = now() where id = :id")
        .param("id", attachmentId)
        .update();
  }

  @Override
  public UUID projectForTask(UUID taskId) {
    return jdbc.sql("select project_id from tasks where id = :id")
        .param("id", taskId)
        .query(UUID.class)
        .optional()
        .orElseThrow(() -> new DomainException("task_not_found", "Task was not found"));
  }

  @Override
  public List<AttachmentView> findByTask(UUID taskId) {
    return jdbc.sql(
            """
            select id, original_name, content_type, size_bytes, created_at
            from attachments where task_id = :task and status = 'READY' order by created_at
            """)
        .param("task", taskId)
        .query(
            (rs, row) ->
                new AttachmentView(
                    rs.getObject("id", UUID.class),
                    rs.getString("original_name"),
                    rs.getString("content_type"),
                    rs.getLong("size_bytes"),
                    rs.getObject("created_at", OffsetDateTime.class)))
        .list();
  }

  private StoredAttachment find(UUID id, String status, String code, String message) {
    return jdbc.sql(
            """
            select project_id, object_key, size_bytes from attachments
            where id = :id and status = :status
            """)
        .param("id", id)
        .param("status", status)
        .query(
            (rs, row) ->
                new StoredAttachment(
                    rs.getObject("project_id", UUID.class),
                    rs.getString("object_key"),
                    rs.getLong("size_bytes")))
        .optional()
        .orElseThrow(() -> new DomainException(code, message));
  }
}
