package com.devsquad.attachment.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.attachment.application.AttachmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.UUID;
import java.util.List;
import com.devsquad.attachment.application.port.AttachmentStore;
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
@RequestMapping("/api/v1/attachments")
public class AttachmentController {
    private final AttachmentService service;

    public AttachmentController(AttachmentService service) { this.service = service; }

    @PostMapping("/upload-ticket")
    public ResponseEntity<AttachmentService.UploadTicket> ticket(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UploadRequest request) {
        var ticket = service.requestUpload(subject(jwt), new AttachmentService.UploadCommand(request.projectId(),
                request.taskId(), request.commentId(), request.fileName(), request.contentType(), request.sizeBytes()));
        return ResponseEntity.created(URI.create("/api/v1/attachments/" + ticket.attachmentId())).body(ticket);
    }

    @PostMapping("/{attachmentId}/complete")
    public ResponseEntity<Void> complete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID attachmentId) {
        service.complete(subject(jwt), attachmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{attachmentId}/download-ticket")
    public AttachmentService.DownloadTicket download(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID attachmentId) {
        return service.download(subject(jwt), attachmentId);
    }

    @GetMapping("/tasks/{taskId}")
    public List<AttachmentStore.AttachmentView> taskAttachments(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId) {
        return service.taskAttachments(subject(jwt), taskId);
    }

    public record UploadRequest(@NotNull UUID projectId, UUID taskId, UUID commentId, @NotBlank String fileName,
                                @NotBlank String contentType, @Positive long sizeBytes) {}
}
