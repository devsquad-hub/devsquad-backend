package com.devsquad.project.adapter.out.persistence;

import com.devsquad.project.application.port.ProjectStore;
import com.devsquad.project.domain.ProjectStatus;
import com.devsquad.shared.persistence.JdbcClient;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JdbcProjectStore implements ProjectStore {

  private static final String SELECT =
      """
      select p.*,
             count(t.id) as total_tasks,
             count(t.id) filter (where wc.semantic_group = 'COMPLETED') as completed_tasks
      from projects p
      left join tasks t on t.project_id = p.id
      left join workflow_columns wc on wc.id = t.column_id
      """;
  private static final String GROUP = " group by p.id ";
  private final JdbcClient jdbc;

  public JdbcProjectStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<ProjectView> find(UUID projectId) {
    return jdbc.sql(SELECT + " where p.id = :id" + GROUP)
        .param("id", projectId)
        .query((rs, row) -> map(rs))
        .optional();
  }

  @Override
  public List<ProjectView> findPublic(UUID hubId) {
    return jdbc.sql(
            SELECT
                + " where p.hub_id = :hubId and p.status <> 'ARCHIVED'"
                + GROUP
                + "order by p.created_at desc")
        .param("hubId", hubId)
        .query((rs, row) -> map(rs))
        .list();
  }

  @Override
  public Optional<ProjectView> findPublicBySlug(String hubSlug, String projectSlug) {
    return jdbc.sql(
            SELECT
                + " join hubs h on h.id = p.hub_id where h.slug = :hubSlug and p.slug ="
                + " :projectSlug"
                + GROUP)
        .param("hubSlug", hubSlug)
        .param("projectSlug", projectSlug)
        .query((rs, row) -> map(rs))
        .optional();
  }

  @Override
  public ProjectView updateStatus(
      UUID projectId, ProjectStatus expectedStatus, ProjectStatus status) {
    var updated =
        jdbc.sql(
                """
                update projects set status = :status, updated_at = now(), version = version + 1
                where id = :id and status = :expectedStatus
                """)
            .param("status", status.name())
            .param("expectedStatus", expectedStatus.name())
            .param("id", projectId)
            .update();
    if (updated == 0) {
      throw new com.devsquad.shared.domain.DomainException(
          "stale_project_status", "Project status changed since it was loaded");
    }
    return find(projectId).orElseThrow();
  }

  @Override
  public ProjectView update(UUID projectId, ProjectUpdate update) {
    jdbc.sql(
            """
            update projects set name = :name, summary = :summary, description = :description,
                repository_url = :repositoryUrl, communication_url = :communicationUrl,
                tags = cast(:tags as text[]), updated_at = now(), version = version + 1
            where id = :id
            """)
        .param("name", update.name().trim())
        .param("summary", update.summary().trim())
        .param("description", update.description())
        .param("repositoryUrl", update.repositoryUrl())
        .param("communicationUrl", update.communicationUrl())
        .param("tags", pgArray(update.tags()))
        .param("id", projectId)
        .update();
    return find(projectId).orElseThrow();
  }

  @Override
  public void assignAdmin(UUID projectId, UUID accountId) {
    jdbc.sql(
            """
            insert into project_memberships (project_id, account_id, role) values (:projectId, :accountId, 'ADMIN')
            on conflict (project_id, account_id) do update set role = 'ADMIN', status = 'ACTIVE', updated_at = now()
            """)
        .param("projectId", projectId)
        .param("accountId", accountId)
        .update();
  }

  @Override
  public boolean isActiveHubMember(UUID hubId, UUID accountId) {
    return jdbc.sql(
                """
                select count(*) from hub_memberships
                where hub_id = :hubId and account_id = :accountId and status = 'ACTIVE'
                """)
            .param("hubId", hubId)
            .param("accountId", accountId)
            .query(Integer.class)
            .single()
        > 0;
  }

  private ProjectView map(ResultSet rs) throws SQLException {
    var array = rs.getArray("tags");
    var tags = array == null ? List.<String>of() : Arrays.asList((String[]) array.getArray());
    var id = rs.getObject("id", UUID.class);
    return new ProjectView(
        id,
        rs.getObject("hub_id", UUID.class),
        rs.getString("name"),
        rs.getString("slug"),
        rs.getString("project_key"),
        rs.getString("summary"),
        rs.getString("description"),
        ProjectStatus.valueOf(rs.getString("status")),
        rs.getString("repository_url"),
        rs.getString("communication_url"),
        List.copyOf(tags),
        rs.getLong("total_tasks"),
        rs.getLong("completed_tasks"),
        members(id));
  }

  private List<MemberView> members(UUID projectId) {
    return jdbc.sql(
            """
            select a.id, a.display_name, a.avatar_url, pm.role, pm.functional_role
            from project_memberships pm join accounts a on a.id = pm.account_id
            where pm.project_id = :projectId and pm.status = 'ACTIVE' order by pm.role, a.display_name
            """)
        .param("projectId", projectId)
        .query(
            (rs, row) ->
                new MemberView(
                    rs.getObject("id", UUID.class),
                    rs.getString("display_name"),
                    rs.getString("avatar_url"),
                    rs.getString("role"),
                    rs.getString("functional_role")))
        .list();
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
}
