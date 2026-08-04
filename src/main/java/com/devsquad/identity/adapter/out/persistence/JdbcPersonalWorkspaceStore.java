package com.devsquad.identity.adapter.out.persistence;

import com.devsquad.identity.application.port.PersonalWorkspaceStore;
import com.devsquad.project.application.port.ProjectStore;
import com.devsquad.shared.persistence.JdbcClient;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class JdbcPersonalWorkspaceStore implements PersonalWorkspaceStore {
  private final JdbcClient jdbc;
  private final ProjectStore projects;

  public JdbcPersonalWorkspaceStore(JdbcClient jdbc, ProjectStore projects) {
    this.jdbc = jdbc;
    this.projects = projects;
  }

  @Override
  public List<ProjectStore.ProjectView> projects(UUID accountId) {
    return jdbc
        .sql(
            """
            select project_id from project_memberships
            where account_id = :accountId and status = 'ACTIVE' order by joined_at desc
            """)
        .param("accountId", accountId)
        .query(UUID.class)
        .list()
        .stream()
        .map(projects::find)
        .flatMap(java.util.Optional::stream)
        .toList();
  }

  @Override
  public List<ApplicationSummary> applications(UUID accountId) {
    return jdbc.sql(
            """
            select pa.id, pa.position_id, pa.status, pa.submitted_at,
                   rp.title as position_title, p.id as project_id, p.name as project_name
            from project_applications pa
            join recruitment_positions rp on rp.id = pa.position_id
            join recruitment_rounds rr on rr.id = rp.round_id
            join projects p on p.id = rr.project_id
            where pa.applicant_id = :accountId order by pa.submitted_at desc
            """)
        .param("accountId", accountId)
        .query(
            (rs, row) ->
                new ApplicationSummary(
                    rs.getObject("id", UUID.class),
                    rs.getObject("project_id", UUID.class),
                    rs.getString("project_name"),
                    rs.getObject("position_id", UUID.class),
                    rs.getString("position_title"),
                    rs.getString("status"),
                    rs.getObject("submitted_at", OffsetDateTime.class)))
        .list();
  }

  @Override
  public List<InvitationSummary> invitations(UUID accountId) {
    return jdbc.sql(
            """
            select pi.id, pi.project_id, p.name as project_name, pi.position_id,
                   pi.functional_role,
                   case when pi.status = 'PENDING' and pi.expires_at <= now()
                        then 'EXPIRED' else pi.status end as status,
                   pi.expires_at
            from project_invitations pi join projects p on p.id = pi.project_id
            where pi.account_id = :accountId order by pi.created_at desc
            """)
        .param("accountId", accountId)
        .query(
            (rs, row) ->
                new InvitationSummary(
                    rs.getObject("id", UUID.class),
                    rs.getObject("project_id", UUID.class),
                    rs.getString("project_name"),
                    rs.getObject("position_id", UUID.class),
                    rs.getString("functional_role"),
                    rs.getString("status"),
                    rs.getObject("expires_at", OffsetDateTime.class)))
        .list();
  }
}
