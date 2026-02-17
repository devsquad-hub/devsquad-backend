package com.spring.boilerplate.domain.project;

import com.spring.boilerplate.api.dto.ProjectRequest;
import com.spring.boilerplate.api.dto.ProjectResponse;
import com.spring.boilerplate.domain.user.UserProfile;
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
