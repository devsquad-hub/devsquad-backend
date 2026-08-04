package com.devsquad.proposal.adapter.out.persistence;

import com.devsquad.proposal.application.port.ProposalStore;
import com.devsquad.proposal.domain.ProjectProposal;
import com.devsquad.proposal.domain.ProposalStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProposalStore implements ProposalStore {

    private static final String VIEW_SELECT = """
            select p.*, a.display_name as author_name from project_proposals p
            join accounts a on a.id = p.author_id
            """;

    private final JdbcClient jdbc;

    public JdbcProposalStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ProposalView create(UUID id, UUID hubId, UUID authorId, ProposalContent content) {
        jdbc.sql("""
                        insert into project_proposals
                            (id, hub_id, author_id, title, summary, problem, proposed_solution, goals, desired_skills, status)
                        values (:id, :hubId, :authorId, :title, :summary, :problem, :solution, :goals,
                                cast(:skills as text[]), 'DRAFT')
                        """)
                .param("id", id).param("hubId", hubId).param("authorId", authorId)
                .param("title", content.title()).param("summary", content.summary())
                .param("problem", content.problem()).param("solution", content.proposedSolution()).param("goals", content.goals())
                .param("skills", pgArray(content.desiredSkills())).update();
        return view(id);
    }

    @Override
    public Optional<ProjectProposal> find(UUID proposalId) {
        return jdbc.sql("""
                        select id, hub_id, author_id, title, summary, status, reviewer_id, decision_reason, project_id
                        from project_proposals where id = :id for update
                        """)
                .param("id", proposalId)
                .query((rs, row) -> ProjectProposal.restore(
                        rs.getObject("id", UUID.class), rs.getObject("hub_id", UUID.class),
                        rs.getObject("author_id", UUID.class), rs.getString("title"), rs.getString("summary"),
                        ProposalStatus.valueOf(rs.getString("status")), rs.getObject("reviewer_id", UUID.class),
                        rs.getString("decision_reason"), rs.getObject("project_id", UUID.class)))
                .optional();
    }

    @Override
    public ProposalView update(ProjectProposal proposal, ProposalContent content) {
        jdbc.sql("""
                        update project_proposals set title = :title, summary = :summary, problem = :problem,
                            proposed_solution = :solution, goals = :goals, desired_skills = cast(:skills as text[]),
                            status = :status, updated_at = now() where id = :id
                        """)
                .param("title", proposal.title()).param("summary", proposal.summary()).param("problem", content.problem())
                .param("solution", content.proposedSolution()).param("goals", content.goals())
                .param("skills", pgArray(content.desiredSkills())).param("status", proposal.status().name())
                .param("id", proposal.id()).update();
        return view(proposal.id());
    }

    @Override
    public ProposalView saveState(ProjectProposal proposal) {
        jdbc.sql("""
                        update project_proposals set status = :status, reviewer_id = :reviewerId,
                            decision_reason = :reason, project_id = :projectId, updated_at = now(),
                            decided_at = case when :status in ('APPROVED', 'REJECTED') then now() else null end
                        where id = :id
                        """)
                .param("status", proposal.status().name()).param("reviewerId", proposal.reviewerId())
                .param("reason", proposal.decisionReason()).param("projectId", proposal.projectId())
                .param("id", proposal.id()).update();
        return view(proposal.id());
    }

    @Override
    public ProposalView approveAndCreate(ProjectProposal proposal, ProposalContent content, String projectSlug, String projectKey) {
        jdbc.sql("""
                        insert into projects (id, hub_id, source_proposal_id, name, slug, project_key, summary, description, status, tags)
                        values (:id, :hubId, :proposalId, :name, :slug, :projectKey, :summary, :description, 'PLANNING', cast(:tags as text[]))
                        """)
                .param("id", proposal.projectId()).param("hubId", proposal.hubId()).param("proposalId", proposal.id())
                .param("name", content.title()).param("slug", projectSlug).param("projectKey", projectKey)
                .param("summary", content.summary()).param("description", content.proposedSolution())
                .param("tags", pgArray(content.desiredSkills())).update();
        jdbc.sql("""
                        insert into project_memberships (project_id, account_id, role)
                        values (:projectId, :authorId, 'ADMIN')
                        """)
                .param("projectId", proposal.projectId()).param("authorId", proposal.authorId()).update();
        createDefaultWorkflow(proposal.projectId());
        saveState(proposal);
        jdbc.sql("""
                        insert into notifications (receiver_id, actor_id, hub_id, project_id, type, title, entity_type, entity_id)
                        values (:receiverId, :actorId, :hubId, :projectId, 'PROPOSAL_APPROVED',
                                'Sua ideia foi aprovada', 'PROJECT_PROPOSAL', :proposalId)
                        """)
                .param("receiverId", proposal.authorId()).param("actorId", proposal.reviewerId())
                .param("hubId", proposal.hubId()).param("projectId", proposal.projectId()).param("proposalId", proposal.id()).update();
        return view(proposal.id());
    }

    @Override
    public Optional<ProposalContent> findContent(UUID proposalId) {
        return jdbc.sql("""
                        select title, summary, problem, proposed_solution, goals, desired_skills
                        from project_proposals where id = :id
                        """)
                .param("id", proposalId)
                .query((rs, row) -> content(rs))
                .optional();
    }

    @Override
    public List<ProposalView> findByHub(UUID hubId, UUID actorId, boolean canReview) {
        var sql = VIEW_SELECT + " where p.hub_id = :hubId " + (canReview ? "" : "and p.author_id = :actorId ")
                + "order by p.created_at desc";
        var query = jdbc.sql(sql).param("hubId", hubId);
        if (!canReview) {
            query = query.param("actorId", actorId);
        }
        return query.query(JdbcProposalStore::mapView).list();
    }

    private ProposalView view(UUID id) {
        return jdbc.sql(VIEW_SELECT + " where p.id = :id")
                .param("id", id).query(JdbcProposalStore::mapView).single();
    }

    private void createDefaultWorkflow(UUID projectId) {
        jdbc.sql("""
                        insert into workflow_columns (project_id, name, semantic_group, position, is_default) values
                            (:projectId, 'Backlog', 'BACKLOG', 0, true),
                            (:projectId, 'Planejado', 'PLANNED', 1, false),
                            (:projectId, 'Em andamento', 'STARTED', 2, false),
                            (:projectId, 'Concluído', 'COMPLETED', 3, false)
                        """).param("projectId", projectId).update();
    }

    private static ProposalView mapView(ResultSet rs, int row) throws SQLException {
        return new ProposalView(rs.getObject("id", UUID.class), rs.getObject("hub_id", UUID.class),
                rs.getObject("author_id", UUID.class), rs.getString("author_name"), content(rs), rs.getString("status"),
                rs.getObject("reviewer_id", UUID.class), rs.getString("decision_reason"), rs.getObject("project_id", UUID.class),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant());
    }

    private static ProposalContent content(ResultSet rs) throws SQLException {
        var value = rs.getArray("desired_skills");
        var skills = value == null ? List.<String>of() : Arrays.asList((String[]) value.getArray());
        return new ProposalContent(rs.getString("title"), rs.getString("summary"), rs.getString("problem"),
                rs.getString("proposed_solution"), rs.getString("goals"), List.copyOf(skills));
    }

    private static String pgArray(List<String> values) {
        return "{" + values.stream().map(value -> "\"" + value.replace("\"", "\\\"") + "\"")
                .reduce((left, right) -> left + "," + right).orElse("") + "}";
    }
}
