package com.devsquad.notification.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.notification.application.NotificationService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/notifications")
public class NotificationController {
  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  @GET
  public List<NotificationService.NotificationView> list(@Context SecurityContext securityContext) {
    return service.list(subject(securityContext));
  }

  @POST
  @Path("/{notificationId}/read")
  public Response read(
      @Context SecurityContext securityContext, @PathParam("notificationId") UUID notificationId) {
    service.markRead(subject(securityContext), notificationId);
    return Response.noContent().build();
  }
}
