package com.spring.devsquad.domain.project;

import com.spring.devsquad.api.dto.ProjectResponse;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {
    public ProjectResponse toResponse(ProjectSummary projectSummary) {
        return new ProjectResponse(
                projectSummary.getId(),
                projectSummary.getTitle(),
                projectSummary.getDescription(),
                projectSummary.getTechStack(),
                projectSummary.getStatus(),
                projectSummary.getUserAuthor().getFirstName(),
                projectSummary.getTotalPositions(),
                projectSummary.getCandidatesCount()
        );
    }
}
