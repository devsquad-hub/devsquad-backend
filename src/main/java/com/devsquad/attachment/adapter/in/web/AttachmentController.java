package com.devsquad.attachment.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.attachment.application.AttachmentService;
import com.devsquad.attachment.application.port.AttachmentStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/attachments")
public class AttachmentController {
  private final AttachmentService service;

  public AttachmentController(AttachmentService service) {
    this.service = service;
  }

  @POST
  @Path("/upload-ticket")
  public Response ticket(@Context SecurityContext securityContext, @Valid UploadRequest request) {
    var ticket =
        service.requestUpload(
            subject(securityContext),
            new AttachmentService.UploadCommand(
                request.projectId(),
                request.taskId(),
                request.commentId(),
                request.fileName(),
                request.contentType(),
                request.sizeBytes()));
    return Response.created(URI.create("/api/v1/attachments/" + ticket.attachmentId()))
        .entity(ticket)
        .build();
  }

  @POST
  @Path("/{attachmentId}/complete")
  public Response complete(
      @Context SecurityContext securityContext, @PathParam("attachmentId") UUID attachmentId) {
    service.complete(subject(securityContext), attachmentId);
    return Response.noContent().build();
  }

  @GET
  @Path("/{attachmentId}/download-ticket")
  public AttachmentService.DownloadTicket download(
      @Context SecurityContext securityContext, @PathParam("attachmentId") UUID attachmentId) {
    return service.download(subject(securityContext), attachmentId);
  }

  @GET
  @Path("/tasks/{taskId}")
  public List<AttachmentStore.AttachmentView> taskAttachments(
      @Context SecurityContext securityContext, @PathParam("taskId") UUID taskId) {
    return service.taskAttachments(subject(securityContext), taskId);
  }

  public record UploadRequest(
      @NotNull UUID projectId,
      UUID taskId,
      UUID commentId,
      @NotBlank String fileName,
      @NotBlank String contentType,
      @Positive long sizeBytes) {}
}
