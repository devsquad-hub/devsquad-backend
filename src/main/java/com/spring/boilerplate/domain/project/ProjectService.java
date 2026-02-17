package com.spring.boilerplate.domain.project;

import com.spring.boilerplate.api.dto.PagedResponse;
import com.spring.boilerplate.api.dto.ProjectRequest;
import com.spring.boilerplate.api.dto.ProjectResponse;
import com.spring.boilerplate.infra.persistence.ProjectRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProjectService {
    private ProjectRepository projectRepository;

    private ProjectMapper projectMapper;

    public ProjectService(ProjectRepository projectRepository, ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
    }

    public PagedResponse<ProjectResponse> getAll(Pageable pageable){
        Page<ProjectSummary> page = projectRepository.findAllProjectSummary(pageable);

        return PagedResponse.<ProjectResponse>builder()
                .page(page.getNumber())
                .size(page.getSize())
                .isLast(page.isLast())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .content(page.getContent().stream()
                        .map(projectSummary -> projectMapper.toResponse(projectSummary))
                        .toList()
                )
                .build();
    }

    public ProjectResponse getProjectById(UUID id) {
        return projectMapper.toResponse(
                projectRepository.findProjectSummaryById(id)
                        .orElseThrow(() -> new RuntimeException("Project not found"))
        );
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        Project project = new Project();
        Project createdProject = projectRepository.save(project);

        ProjectSummary projectSummary = projectRepository.findProjectSummaryById(createdProject.getId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        return projectMapper.toResponse(projectSummary);
    }
}
