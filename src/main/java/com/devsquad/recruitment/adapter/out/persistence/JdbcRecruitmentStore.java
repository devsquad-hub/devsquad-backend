package com.devsquad.recruitment.adapter.out.persistence;

import com.devsquad.recruitment.application.RecruitmentService.ApplicationView;
import com.devsquad.recruitment.application.RecruitmentService.PositionCommand;
import com.devsquad.recruitment.application.RecruitmentService.PositionView;
import com.devsquad.recruitment.application.RecruitmentService.QuestionView;
import com.devsquad.recruitment.application.RecruitmentService.RoundCommand;
import com.devsquad.recruitment.application.port.RecruitmentStore;
import com.devsquad.shared.domain.DomainException;
import com.devsquad.shared.persistence.JdbcClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class JdbcRecruitmentStore implements RecruitmentStore {
  private final JdbcClient jdbc;
  private final ObjectMapper mapper;

  public JdbcRecruitmentStore(JdbcClient jdbc, ObjectMapper mapper) {
    this.jdbc = jdbc;
    this.mapper = mapper;
  }

  @Override
  public UUID createRound(UUID projectId, RoundCommand command) {
    var id = UUID.randomUUID();
    jdbc.sql(
            """
            insert into recruitment_rounds (id, project_id, name, description, status, opens_at, closes_at)
            values (:id, :projectId, :name, :description, 'DRAFT', :opensAt, :closesAt)
            """)
        .param("id", id)
        .param("projectId", projectId)
        .param("name", command.name())
        .param("description", command.description())
        .param("opensAt", command.opensAt())
        .param("closesAt", command.closesAt())
        .update();
    return id;
  }

  @Override
  public UUID projectForRound(UUID roundId) {
    return jdbc.sql("select project_id from recruitment_rounds where id = :id")
        .param("id", roundId)
        .query(UUID.class)
        .optional()
        .orElseThrow(
            () -> new DomainException("recruitment_round_not_found", "Round was not found"));
  }

  @Override
  public UUID createPosition(UUID roundId, PositionCommand command) {
    var positionId = UUID.randomUUID();
    var formId = UUID.randomUUID();
    jdbc.sql(
            """
            insert into recruitment_positions (id, round_id, title, description, skills, capacity)
            values (:id, :roundId, :title, :description, cast(:skills as text[]), :capacity)
            """)
        .param("id", positionId)
        .param("roundId", roundId)
        .param("title", command.title())
        .param("description", command.description())
        .param("skills", pgArray(command.skills()))
        .param("capacity", command.capacity())
        .update();
    jdbc.sql(
            "insert into recruitment_form_versions (id, position_id, version) values (:id,"
                + " :positionId, 1)")
        .param("id", formId)
        .param("positionId", positionId)
        .update();
    var order = 0;
    for (var question : command.questions()) {
      jdbc.sql(
              """
              insert into recruitment_questions
                  (form_version_id, question_key, label, type, required, position, options)
              values (:formId, :key, :label, :type, :required, :position, cast(:options as jsonb))
              """)
          .param("formId", formId)
          .param("key", question.key())
          .param("label", question.label())
          .param("type", question.type())
          .param("required", question.required())
          .param("position", order++)
          .param("options", json(question.options()))
          .update();
    }
    return positionId;
  }

  @Override
  public void openRound(UUID roundId, UUID projectId) {
    var opened =
        jdbc.sql(
                """
                update recruitment_rounds set status = 'OPEN', opens_at = coalesce(opens_at, now()),
                    updated_at = now() where id = :id and status = 'DRAFT'
                  and exists (select 1 from projects where id = :project and status in ('PLANNING', 'RECRUITING'))
                """)
            .param("id", roundId)
            .param("project", projectId)
            .update();
    if (opened == 0) {
      throw new DomainException(
          "round_not_openable", "Round or project is not in an openable state");
    }
    jdbc.sql(
            "update projects set status = 'RECRUITING', updated_at = now() where id = :id and"
                + " status = 'PLANNING'")
        .param("id", projectId)
        .update();
  }

  @Override
  public List<PositionView> publicPositions(UUID projectId) {
    return jdbc.sql(
            """
            select rp.id, rp.title, rp.description, rp.skills, rp.capacity, rp.filled,
                   rr.id as round_id, rr.name as round_name, rr.closes_at
            from recruitment_positions rp join recruitment_rounds rr on rr.id = rp.round_id
            where rr.project_id = :projectId and rr.status = 'OPEN' and rp.status = 'OPEN'
              and (rr.opens_at is null or rr.opens_at <= now())
              and (rr.closes_at is null or rr.closes_at > now()) order by rr.created_at, rp.created_at
            """)
        .param("projectId", projectId)
        .query(
            (rs, row) ->
                new PositionView(
                    rs.getObject("id", UUID.class),
                    rs.getObject("round_id", UUID.class),
                    rs.getString("round_name"),
                    rs.getString("title"),
                    rs.getString("description"),
                    List.of((String[]) rs.getArray("skills").getArray()),
                    rs.getInt("capacity"),
                    rs.getInt("filled"),
                    rs.getObject("closes_at", OffsetDateTime.class),
                    questions(rs.getObject("id", UUID.class))))
        .list();
  }

  private List<QuestionView> questions(UUID positionId) {
    return jdbc.sql(
            """
            select question_key, label, type, required, options::text
            from recruitment_questions
            where form_version_id = (
                select id from recruitment_form_versions
                where position_id = :position order by version desc limit 1
            ) order by position
            """)
        .param("position", positionId)
        .query(
            (rs, row) ->
                new QuestionView(
                    rs.getString("question_key"),
                    rs.getString("label"),
                    rs.getString("type"),
                    rs.getBoolean("required"),
                    jsonArray(rs.getString("options"))))
        .list();
  }

  @Override
  public PositionContext positionContext(UUID positionId) {
    return jdbc.sql(
            """
            select rr.project_id, p.hub_id from recruitment_positions rp
            join recruitment_rounds rr on rr.id = rp.round_id join projects p on p.id = rr.project_id
            where rp.id = :id and rp.status = 'OPEN' and rr.status = 'OPEN'
              and (rr.opens_at is null or rr.opens_at <= now())
              and (rr.closes_at is null or rr.closes_at > now())
            """)
        .param("id", positionId)
        .query(
            (rs, row) ->
                new PositionContext(
                    rs.getObject("project_id", UUID.class), rs.getObject("hub_id", UUID.class)))
        .optional()
        .orElseThrow(
            () -> new DomainException("position_not_open", "Recruitment position is not open"));
  }

  @Override
  public UUID apply(UUID positionId, UUID projectId, UUID applicantId, Object answers) {
    var member =
        jdbc.sql(
                """
                select count(*) from project_memberships
                where project_id = :projectId and account_id = :accountId and status = 'ACTIVE'
                """)
            .param("projectId", projectId)
            .param("accountId", applicantId)
            .query(Integer.class)
            .single();
    if (member > 0) {
      throw new DomainException("already_project_member", "Project members cannot apply again");
    }
    var formId =
        jdbc.sql(
                """
                select id from recruitment_form_versions
                where position_id = :id order by version desc limit 1
                """)
            .param("id", positionId)
            .query(UUID.class)
            .optional()
            .orElseThrow(
                () ->
                    new DomainException(
                        "recruitment_form_not_found", "Recruitment form was not found"));
    validateRequiredAnswers(formId, answers);
    var id = UUID.randomUUID();
    jdbc.sql(
            """
            insert into project_applications
                (id, position_id, form_version_id, applicant_id, answers, status)
            values (:id, :positionId, :formId, :applicantId, cast(:answers as jsonb), 'SUBMITTED')
            """)
        .param("id", id)
        .param("positionId", positionId)
        .param("formId", formId)
        .param("applicantId", applicantId)
        .param("answers", json(answers))
        .update();
    return id;
  }

  @Override
  public ApplicationContext applicationContext(UUID applicationId) {
    return jdbc.sql(
            """
            select pa.position_id, pa.applicant_id, rr.project_id
            from project_applications pa join recruitment_positions rp on rp.id = pa.position_id
            join recruitment_rounds rr on rr.id = rp.round_id
            where pa.id = :id and pa.status = 'SUBMITTED'
            """)
        .param("id", applicationId)
        .query(
            (rs, row) ->
                new ApplicationContext(
                    rs.getObject("position_id", UUID.class),
                    rs.getObject("applicant_id", UUID.class),
                    rs.getObject("project_id", UUID.class)))
        .optional()
        .orElseThrow(
            () -> new DomainException("application_not_pending", "Application is not pending"));
  }

  @Override
  public void decide(
      UUID applicationId, ApplicationContext row, UUID reviewerId, boolean accept, String note) {
    var decided =
        jdbc.sql(
                """
                update project_applications set status = :status, reviewer_id = :reviewerId,
                    decision_note = :note, decided_at = now() where id = :id and status = 'SUBMITTED'
                """)
            .param("status", accept ? "ACCEPTED" : "REJECTED")
            .param("reviewerId", reviewerId)
            .param("note", note)
            .param("id", applicationId)
            .update();
    if (decided == 0) {
      throw new DomainException("application_not_pending", "Application is not pending");
    }
    if (accept) {
      reservePosition(row.positionId());
      activateMembership(row.projectId(), row.applicantId(), null);
    }
    jdbc.sql(
            """
            insert into notifications
                (receiver_id, actor_id, project_id, type, title, entity_type, entity_id)
            values (:receiver, :actor, :project, :type, :title, 'PROJECT_APPLICATION', :application)
            """)
        .param("receiver", row.applicantId())
        .param("actor", reviewerId)
        .param("project", row.projectId())
        .param("type", accept ? "APPLICATION_ACCEPTED" : "APPLICATION_REJECTED")
        .param("title", accept ? "Sua inscrição foi aprovada" : "Sua inscrição não foi aprovada")
        .param("application", applicationId)
        .update();
  }

  @Override
  public List<ApplicationView> applications(UUID projectId) {
    return jdbc.sql(
            """
            select pa.id, pa.position_id, pa.applicant_id, a.display_name, a.avatar_url,
                   rp.title as position_title, pa.answers::text, pa.status, pa.submitted_at
            from project_applications pa join recruitment_positions rp on rp.id = pa.position_id
            join recruitment_rounds rr on rr.id = rp.round_id join accounts a on a.id = pa.applicant_id
            where rr.project_id = :projectId order by pa.submitted_at
            """)
        .param("projectId", projectId)
        .query(
            (rs, row) ->
                new ApplicationView(
                    rs.getObject("id", UUID.class),
                    rs.getObject("position_id", UUID.class),
                    rs.getString("position_title"),
                    rs.getObject("applicant_id", UUID.class),
                    rs.getString("display_name"),
                    rs.getString("avatar_url"),
                    rs.getString("answers"),
                    rs.getString("status"),
                    rs.getObject("submitted_at", OffsetDateTime.class)))
        .list();
  }

  @Override
  public UUID invite(
      UUID projectId, UUID accountId, UUID positionId, String functionalRole, UUID inviterId) {
    requireInvitee(projectId, accountId);
    requireOptionalPosition(projectId, positionId);
    jdbc.sql(
            """
            update project_invitations set status = 'EXPIRED'
            where project_id = :project and account_id = :account
              and status = 'PENDING' and expires_at <= now()
            """)
        .param("project", projectId)
        .param("account", accountId)
        .update();
    var id = UUID.randomUUID();
    jdbc.sql(
            """
            insert into project_invitations
                (id, project_id, position_id, account_id, invited_by, functional_role, status, expires_at)
            values (:id, :project, :position, :account, :inviter, :role, 'PENDING', now() + interval '7 days')
            """)
        .param("id", id)
        .param("project", projectId)
        .param("position", positionId)
        .param("account", accountId)
        .param("inviter", inviterId)
        .param("role", functionalRole)
        .update();
    jdbc.sql(
            """
            insert into notifications
                (receiver_id, actor_id, project_id, type, title, entity_type, entity_id)
            values (:receiver, :actor, :project, 'PROJECT_INVITATION',
                    'Você recebeu um convite para projeto', 'PROJECT_INVITATION', :invitation)
            """)
        .param("receiver", accountId)
        .param("actor", inviterId)
        .param("project", projectId)
        .param("invitation", id)
        .update();
    return id;
  }

  @Override
  public void respondToInvitation(UUID invitationId, UUID accountId, boolean accept) {
    var invitation =
        jdbc.sql(
                """
                select project_id, position_id, functional_role from project_invitations
                where id = :id and account_id = :account and status = 'PENDING' and expires_at > now()
                """)
            .param("id", invitationId)
            .param("account", accountId)
            .query(
                (rs, row) ->
                    new InvitationContext(
                        rs.getObject("project_id", UUID.class),
                        rs.getObject("position_id", UUID.class),
                        rs.getString("functional_role")))
            .optional()
            .orElseThrow(
                () -> new DomainException("invitation_not_pending", "Invitation is not pending"));
    var responded =
        jdbc.sql(
                """
                update project_invitations set status = :status, responded_at = now()
                where id = :id and account_id = :account and status = 'PENDING' and expires_at > now()
                """)
            .param("status", accept ? "ACCEPTED" : "DECLINED")
            .param("id", invitationId)
            .param("account", accountId)
            .update();
    if (responded == 0) {
      throw new DomainException("invitation_not_pending", "Invitation is not pending");
    }
    if (accept) {
      if (invitation.positionId() != null) reservePosition(invitation.positionId());
      activateMembership(invitation.projectId(), accountId, invitation.functionalRole());
    }
  }

  private void requireInvitee(UUID projectId, UUID accountId) {
    var count =
        jdbc.sql(
                """
                select count(*) from hub_memberships hm join projects p on p.hub_id = hm.hub_id
                where p.id = :project and hm.account_id = :account and hm.status = 'ACTIVE'
                """)
            .param("project", projectId)
            .param("account", accountId)
            .query(Integer.class)
            .single();
    if (count == 0) {
      throw new DomainException("invitee_not_hub_member", "Invitee must be an active hub member");
    }
  }

  private void requireOptionalPosition(UUID projectId, UUID positionId) {
    if (positionId == null) return;
    var count =
        jdbc.sql(
                """
                select count(*) from recruitment_positions rp join recruitment_rounds rr on rr.id = rp.round_id
                where rp.id = :position and rr.project_id = :project
                """)
            .param("position", positionId)
            .param("project", projectId)
            .query(Integer.class)
            .single();
    if (count == 0) {
      throw new DomainException("position_not_found", "Position is not in this project");
    }
  }

  private void reservePosition(UUID positionId) {
    var reserved =
        jdbc.sql(
                """
                update recruitment_positions set filled = filled + 1, version = version + 1,
                    status = case when filled + 1 = capacity then 'FILLED' else status end
                where id = :id and filled < capacity and status = 'OPEN'
                """)
            .param("id", positionId)
            .update();
    if (reserved == 0) {
      throw new DomainException("position_full", "Recruitment position is full");
    }
  }

  private void activateMembership(UUID projectId, UUID accountId, String functionalRole) {
    jdbc.sql(
            """
            insert into project_memberships (project_id, account_id, role, functional_role)
            values (:project, :account, 'MEMBER', :role)
            on conflict (project_id, account_id) do update set status = 'ACTIVE',
                functional_role = coalesce(excluded.functional_role, project_memberships.functional_role),
                updated_at = now()
            """)
        .param("project", projectId)
        .param("account", accountId)
        .param("role", functionalRole)
        .update();
  }

  private void validateRequiredAnswers(UUID formId, Object answers) {
    if (!(answers instanceof Map<?, ?> answerMap)) {
      throw new DomainException(
          "invalid_application_answers", "Answers must be an object keyed by question");
    }
    if (answerMap.size() > 30 || json(answers).length() > 100_000) {
      throw new DomainException(
          "application_answers_too_large", "Application answers exceed the allowed size");
    }
    var definitions =
        jdbc.sql(
                """
                select question_key, type, required, options::text
                from recruitment_questions where form_version_id = :form
                """)
            .param("form", formId)
            .query(
                (rs, row) ->
                    new AnswerDefinition(
                        rs.getString("question_key"),
                        rs.getString("type"),
                        rs.getBoolean("required"),
                        jsonArray(rs.getString("options"))))
            .list();
    if (definitions.isEmpty()) {
      var motivation = answerMap.get("motivation");
      if (!answerMap.keySet().stream().allMatch("motivation"::equals)
          || (motivation != null
              && (!(motivation instanceof String) || ((String) motivation).length() > 4_000))) {
        throw new DomainException(
            "invalid_application_answers", "Application answers do not match the form");
      }
      return;
    }
    var knownKeys =
        definitions.stream()
            .map(AnswerDefinition::key)
            .collect(java.util.stream.Collectors.toSet());
    if (!knownKeys.containsAll(answerMap.keySet())) {
      throw new DomainException(
          "invalid_application_answers", "Application contains unknown answers");
    }
    for (var definition : definitions) {
      validateAnswer(definition, answerMap.get(definition.key()));
    }
  }

  private void validateAnswer(AnswerDefinition definition, Object value) {
    if (value == null || (value instanceof String text && text.isBlank())) {
      if (definition.required()) {
        throw new DomainException(
            "required_answer_missing", "Required answer is missing: " + definition.key());
      }
      return;
    }
    var valid =
        switch (definition.type()) {
          case "BOOLEAN" -> value instanceof Boolean;
          case "SINGLE_CHOICE" ->
              value instanceof String text && definition.options().contains(text);
          case "MULTIPLE_CHOICE" ->
              value instanceof List<?> values
                  && values.size() <= 30
                  && values.stream()
                      .allMatch(
                          item ->
                              item instanceof String text && definition.options().contains(text));
          case "SHORT_TEXT" -> value instanceof String text && text.length() <= 500;
          case "URL" -> value instanceof String text && text.length() <= 2_000;
          case "LONG_TEXT" -> value instanceof String text && text.length() <= 10_000;
          default -> false;
        };
    if (!valid) {
      throw new DomainException(
          "invalid_application_answer", "Answer has an invalid type or value: " + definition.key());
    }
  }

  private String json(Object value) {
    try {
      return mapper.writeValueAsString(value == null ? List.of() : value);
    } catch (Exception exception) {
      throw new DomainException("invalid_json", "Value cannot be encoded");
    }
  }

  private List<String> jsonArray(String value) {
    try {
      return List.of(mapper.readValue(value, String[].class));
    } catch (Exception exception) {
      throw new DomainException("invalid_json", "Stored options cannot be decoded");
    }
  }

  private static String pgArray(List<String> values) {
    if (values == null) return "{}";
    return "{"
        + values.stream()
            .map(value -> "\"" + value.replace("\"", "\\\"") + "\"")
            .reduce((left, right) -> left + "," + right)
            .orElse("")
        + "}";
  }

  private record InvitationContext(UUID projectId, UUID positionId, String functionalRole) {}

  private record AnswerDefinition(
      String key, String type, boolean required, List<String> options) {}
}
