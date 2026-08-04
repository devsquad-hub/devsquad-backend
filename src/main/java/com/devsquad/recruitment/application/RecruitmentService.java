package com.devsquad.recruitment.application;

import com.devsquad.recruitment.application.port.RecruitmentStore;
import com.devsquad.shared.domain.DomainException;
import com.devsquad.shared.security.AuthorizationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class RecruitmentService {
  private static final Set<String> QUESTION_TYPES =
      Set.of("SHORT_TEXT", "LONG_TEXT", "URL", "SINGLE_CHOICE", "MULTIPLE_CHOICE", "BOOLEAN");

  private final RecruitmentStore recruitment;
  private final AuthorizationService authorization;

  public RecruitmentService(RecruitmentStore recruitment, AuthorizationService authorization) {
    this.recruitment = recruitment;
    this.authorization = authorization;
  }

  @Transactional
  public UUID createRound(String clerkId, UUID projectId, RoundCommand command) {
    authorization.requireProjectAdmin(clerkId, projectId);
    return recruitment.createRound(projectId, command);
  }

  @Transactional
  public UUID createPosition(String clerkId, UUID roundId, PositionCommand command) {
    for (var question : command.questions()) {
      if (!QUESTION_TYPES.contains(question.type())) {
        throw new DomainException("invalid_question_type", "Recruitment question type is invalid");
      }
    }
    authorization.requireProjectAdmin(clerkId, recruitment.projectForRound(roundId));
    return recruitment.createPosition(roundId, command);
  }

  @Transactional
  public void openRound(String clerkId, UUID roundId) {
    var projectId = recruitment.projectForRound(roundId);
    authorization.requireProjectAdmin(clerkId, projectId);
    recruitment.openRound(roundId, projectId);
  }

  public List<PositionView> publicPositions(UUID projectId) {
    return recruitment.publicPositions(projectId);
  }

  @Transactional
  public UUID apply(String clerkId, UUID positionId, Object answers) {
    var context = recruitment.positionContext(positionId);
    var applicantId = authorization.requireHubMember(clerkId, context.hubId());
    return recruitment.apply(positionId, context.projectId(), applicantId, answers);
  }

  @Transactional
  public void decide(String clerkId, UUID applicationId, boolean accept, String note) {
    var context = recruitment.applicationContext(applicationId);
    var reviewerId = authorization.requireProjectAdmin(clerkId, context.projectId());
    recruitment.decide(applicationId, context, reviewerId, accept, note);
  }

  public List<ApplicationView> applications(String clerkId, UUID projectId) {
    authorization.requireProjectAdmin(clerkId, projectId);
    return recruitment.applications(projectId);
  }

  @Transactional
  public UUID invite(
      String clerkId, UUID projectId, UUID accountId, UUID positionId, String functionalRole) {
    var inviterId = authorization.requireProjectAdmin(clerkId, projectId);
    return recruitment.invite(projectId, accountId, positionId, functionalRole, inviterId);
  }

  @Transactional
  public void respondToInvitation(String clerkId, UUID invitationId, boolean accept) {
    recruitment.respondToInvitation(invitationId, authorization.requireAccount(clerkId), accept);
  }

  public record RoundCommand(String name, String description, Instant opensAt, Instant closesAt) {}

  public record PositionCommand(
      String title,
      String description,
      List<String> skills,
      int capacity,
      List<Question> questions) {
    public PositionCommand {
      questions = questions == null ? List.of() : List.copyOf(questions);
    }
  }

  public record Question(
      String key, String label, String type, boolean required, List<String> options) {}

  public record PositionView(
      UUID id,
      UUID roundId,
      String roundName,
      String title,
      String description,
      List<String> skills,
      int capacity,
      int filled,
      OffsetDateTime closesAt,
      List<QuestionView> questions) {}

  public record QuestionView(
      String key, String label, String type, boolean required, List<String> options) {}

  public record ApplicationView(
      UUID id,
      UUID positionId,
      String positionTitle,
      UUID applicantId,
      String applicantName,
      String applicantAvatarUrl,
      String answersJson,
      String status,
      OffsetDateTime submittedAt) {}
}
