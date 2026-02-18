package com.spring.devsquad.infra.persistence;

import com.spring.devsquad.domain.project.Project;
import com.spring.devsquad.domain.project.ProjectSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
//    TODO create JPQL query
        @Query(
        """
        SELECT p.id AS id,
               p.title AS title,
               p.description AS description,
               p.techStack AS techStack,
               p.status AS status,
               p.userAuthor AS userAuthor,
               COUNT(DISTINCT sa) AS candidatesCount
               FROM Project p
               
               LEFT JOIN SquadApplication sa
               ON sa.project.id = p.id
               
               WHERE p.id = :projectId
               GROUP BY p.id
        """
)
    Optional<ProjectSummary> findProjectSummaryById(@Param("projectId") UUID id);

//    TODO create JPQL query
    @Query(
        """
        SELECT p.id AS id,
               p.title AS title,
               p.description AS description,
               p.techStack AS techStack,
               p.status AS status,
               p.userAuthor AS userAuthor,
               COUNT(DISTINCT sa) AS candidatesCount
               FROM Project p
               
               LEFT JOIN SquadApplication sa
               ON sa.project.id = p.id
               GROUP BY p.id
        """
    )
    Page<ProjectSummary> findAllProjectSummary(Pageable pageable);
}
