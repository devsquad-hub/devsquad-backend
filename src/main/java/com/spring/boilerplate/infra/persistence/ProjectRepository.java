package com.spring.boilerplate.infra.persistence;

import com.spring.boilerplate.domain.project.Project;
import com.spring.boilerplate.domain.project.ProjectSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
//    TODO create JPQL query
    Optional<ProjectSummary> findProjectSummaryById(@Param("projectId") UUID id);

//    TODO create JPQL query
    Page<ProjectSummary> findAllProjectSummary(Pageable pageable);
}
