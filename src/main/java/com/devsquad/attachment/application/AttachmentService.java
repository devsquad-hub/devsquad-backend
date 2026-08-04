package com.devsquad.attachment.application;

import com.devsquad.attachment.application.port.AttachmentStorage;
import com.devsquad.attachment.application.port.AttachmentStore;
import com.devsquad.shared.domain.DomainException;
import com.devsquad.shared.security.AuthorizationService;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttachmentService {
    private final AttachmentStore attachments;
    private final AttachmentStorage storage;
    private final AuthorizationService authorization;

    public AttachmentService(
            AttachmentStore attachments,
            AttachmentStorage storage,
            AuthorizationService authorization) {
        this.attachments = attachments;
        this.storage = storage;
        this.authorization = authorization;
    }

    @Transactional
    public UploadTicket requestUpload(String clerkId, UploadCommand command) {
        requireValid(command);
        var actorId = authorization.requireProjectMember(clerkId, command.projectId());
        if (!attachments.parentBelongsToProject(command)) {
            throw new DomainException("invalid_attachment_parent", "Attachment parent is not in this project");
        }

        var id = UUID.randomUUID();
        var objectKey = command.projectId() + "/" + id + "/" + safeName(command.fileName());
        attachments.create(id, actorId, objectKey, command);
        var signed = storage.signUpload(objectKey, command.contentType(), command.sizeBytes());
        return new UploadTicket(id, signed.url(), signed.headers());
    }

    @Transactional
    public void complete(String clerkId, UUID attachmentId) {
        var attachment = attachments.pending(attachmentId);
        authorization.requireProjectMember(clerkId, attachment.projectId());
        if (storage.objectSize(attachment.objectKey()) != attachment.sizeBytes()) {
            throw new DomainException("attachment_size_mismatch", "Uploaded object size does not match");
        }
        attachments.markReady(attachmentId);
    }

    public DownloadTicket download(String clerkId, UUID attachmentId) {
        var attachment = attachments.ready(attachmentId);
        authorization.requireProjectMember(clerkId, attachment.projectId());
        return new DownloadTicket(storage.signDownload(attachment.objectKey()));
    }

    public List<AttachmentStore.AttachmentView> taskAttachments(String clerkId, UUID taskId) {
        authorization.requireProjectMember(clerkId, attachments.projectForTask(taskId));
        return attachments.findByTask(taskId);
    }

    private void requireValid(UploadCommand command) {
        if ((command.taskId() == null) == (command.commentId() == null)) {
            throw new DomainException("invalid_attachment_parent", "Choose exactly one task or comment");
        }
        if (command.sizeBytes() <= 0 || command.sizeBytes() > storage.maxFileSize()) {
            throw new DomainException("invalid_attachment_size", "Attachment size is outside the allowed range");
        }
    }

    private static String safeName(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException("invalid_file_name", "File name is required");
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record UploadCommand(
            UUID projectId, UUID taskId, UUID commentId, String fileName, String contentType, long sizeBytes) {}
    public record UploadTicket(UUID attachmentId, String uploadUrl, Map<String, java.util.List<String>> headers) {}
    public record DownloadTicket(String downloadUrl) {}
}
