package com.devsquad.attachment.application.port;

import com.devsquad.attachment.application.AttachmentService.UploadCommand;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AttachmentStore {
    boolean parentBelongsToProject(UploadCommand command);

    void create(UUID id, UUID actorId, String objectKey, UploadCommand command);

    StoredAttachment pending(UUID attachmentId);

    StoredAttachment ready(UUID attachmentId);

    void markReady(UUID attachmentId);

    UUID projectForTask(UUID taskId);

    List<AttachmentView> findByTask(UUID taskId);

    record StoredAttachment(UUID projectId, String objectKey, long sizeBytes) {}
    record AttachmentView(UUID id, String originalName, String contentType, long sizeBytes,
                          OffsetDateTime createdAt) {}
}
