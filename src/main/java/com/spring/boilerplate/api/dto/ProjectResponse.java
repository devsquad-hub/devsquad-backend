package com.spring.boilerplate.api.dto;

import com.spring.boilerplate.domain.project.ProjectStatus;

import java.util.List;
import java.util.UUID;

public record ProjectResponse (
    UUID id,
    String title,
    String description,
//    List<String> stack,
    String techStack,
    ProjectStatus status,
    String authorName,
    Integer totalPositions,
    Integer candidatesCount
){
}
