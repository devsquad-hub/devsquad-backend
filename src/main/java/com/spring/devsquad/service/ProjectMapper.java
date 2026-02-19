package com.spring.devsquad.service;

import com.spring.devsquad.dto.ProjectResponse;
import com.spring.devsquad.dto.ProjectSummary;
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
//                projectSummary.getTotalPositions(),
                projectSummary.getCandidatesCount()
        );
    }
}
