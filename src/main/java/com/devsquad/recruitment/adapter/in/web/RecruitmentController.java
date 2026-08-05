package com.devsquad.recruitment.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.recruitment.application.RecruitmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1")
public class RecruitmentController {

  private final RecruitmentService service;

  public RecruitmentController(RecruitmentService service) {
    this.service = service;
  }

  @POST
  @Path("/projects/{projectId}/recruitment-rounds")
  public Response createRound(
      @Context SecurityContext securityContext,
      @PathParam("projectId") UUID projectId,
      @Valid RoundRequest request) {
    var id =
        service.createRound(
            subject(securityContext),
            projectId,
            new RecruitmentService.RoundCommand(
                request.name(), request.description(), request.opensAt(), request.closesAt()));
    return Response.created(URI.create("/api/v1/recruitment-rounds/" + id))
        .entity(new IdResponse(id))
        .build();
  }

  @POST
  @Path("/recruitment-rounds/{roundId}/positions")
  public Response createPosition(
      @Context SecurityContext securityContext,
      @PathParam("roundId") UUID roundId,
      @Valid PositionRequest request) {
    var questions =
        request.questions() == null
            ? List.<RecruitmentService.Question>of()
            : request.questions().stream()
                .map(
                    question ->
                        new RecruitmentService.Question(
                            question.key(),
                            question.label(),
                            question.type(),
                            question.required(),
                            question.options()))
                .toList();
    var id =
        service.createPosition(
            subject(securityContext),
            roundId,
            new RecruitmentService.PositionCommand(
                request.title(),
                request.description(),
                request.skills(),
                request.capacity(),
                questions));
    return Response.created(URI.create("/api/v1/recruitment-positions/" + id))
        .entity(new IdResponse(id))
        .build();
  }

  @POST
  @Path("/recruitment-rounds/{roundId}/open")
  public Response open(
      @Context SecurityContext securityContext, @PathParam("roundId") UUID roundId) {
    service.openRound(subject(securityContext), roundId);
    return Response.noContent().build();
  }

  @POST
  @Path("/recruitment-positions/{positionId}/applications")
  public Response apply(
      @Context SecurityContext securityContext,
      @PathParam("positionId") UUID positionId,
      Map<String, Object> answers) {
    var id = service.apply(subject(securityContext), positionId, answers);
    return Response.created(URI.create("/api/v1/applications/" + id))
        .entity(new IdResponse(id))
        .build();
  }

  @POST
  @Path("/applications/{applicationId}/accept")
  public Response accept(
      @Context SecurityContext securityContext,
      @PathParam("applicationId") UUID applicationId,
      DecisionRequest request) {
    service.decide(
        subject(securityContext), applicationId, true, request == null ? null : request.note());
    return Response.noContent().build();
  }

  @POST
  @Path("/applications/{applicationId}/reject")
  public Response reject(
      @Context SecurityContext securityContext,
      @PathParam("applicationId") UUID applicationId,
      DecisionRequest request) {
    service.decide(
        subject(securityContext), applicationId, false, request == null ? null : request.note());
    return Response.noContent().build();
  }

  @GET
  @Path("/projects/{projectId}/applications")
  public List<RecruitmentService.ApplicationView> applications(
      @Context SecurityContext securityContext, @PathParam("projectId") UUID projectId) {
    return service.applications(subject(securityContext), projectId);
  }

  @POST
  @Path("/projects/{projectId}/invitations")
  public Response invite(
      @Context SecurityContext securityContext,
      @PathParam("projectId") UUID projectId,
      @Valid InviteRequest request) {
    var id =
        service.invite(
            subject(securityContext),
            projectId,
            request.accountId(),
            request.positionId(),
            request.functionalRole());
    return Response.created(URI.create("/api/v1/invitations/" + id))
        .entity(new IdResponse(id))
        .build();
  }

  @POST
  @Path("/invitations/{invitationId}/accept")
  public Response acceptInvitation(
      @Context SecurityContext securityContext, @PathParam("invitationId") UUID invitationId) {
    service.respondToInvitation(subject(securityContext), invitationId, true);
    return Response.noContent().build();
  }

  @POST
  @Path("/invitations/{invitationId}/decline")
  public Response declineInvitation(
      @Context SecurityContext securityContext, @PathParam("invitationId") UUID invitationId) {
    service.respondToInvitation(subject(securityContext), invitationId, false);
    return Response.noContent().build();
  }

  public record RoundRequest(
      @NotBlank @Size(max = 160) String name,
      @Size(max = 4_000) String description,
      Instant opensAt,
      Instant closesAt) {}

  public record PositionRequest(
      @NotBlank @Size(max = 140) String title,
      @Size(max = 4_000) String description,
      @Size(max = 30) List<String> skills,
      @Min(1) int capacity,
      @Size(max = 30) List<@Valid QuestionRequest> questions) {}

  public record QuestionRequest(
      @NotBlank @Size(max = 64) String key,
      @NotBlank @Size(max = 500) String label,
      @NotBlank String type,
      boolean required,
      List<String> options) {}

  public record DecisionRequest(@Size(max = 2_000) String note) {}

  public record InviteRequest(
      @NotNull UUID accountId, UUID positionId, @Size(max = 120) String functionalRole) {}

  public record IdResponse(UUID id) {}
}
