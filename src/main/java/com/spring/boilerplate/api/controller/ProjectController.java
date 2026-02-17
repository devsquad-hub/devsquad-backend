package com.spring.boilerplate.api.controller;

import com.spring.boilerplate.api.dto.ApiResponse;
import com.spring.boilerplate.api.dto.PagedResponse;
import com.spring.boilerplate.api.dto.ProjectRequest;
import com.spring.boilerplate.api.dto.ProjectResponse;
import com.spring.boilerplate.domain.project.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
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





