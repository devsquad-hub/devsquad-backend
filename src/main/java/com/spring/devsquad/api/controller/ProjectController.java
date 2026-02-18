package com.spring.devsquad.api.controller;

import com.spring.devsquad.api.dto.ApiResponse;
import com.spring.devsquad.api.dto.PagedResponse;
import com.spring.devsquad.api.dto.ProjectRequest;
import com.spring.devsquad.api.dto.ProjectResponse;
import com.spring.devsquad.domain.project.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(@PathVariable UUID id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(projectService.getProjectById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProjectResponse>>> getAllProjects(Pageable pageable) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(projectService.getAll(pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@Valid @RequestBody ProjectRequest projectRequest) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(projectService.create(projectRequest)));
    }
}





