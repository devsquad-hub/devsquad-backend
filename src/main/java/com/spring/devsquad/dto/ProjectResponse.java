package com.spring.devsquad.dto;

import com.spring.devsquad.model.ProjectStatus;

import java.util.UUID;

public record ProjectResponse (
    UUID id,
    String title,
    String description,
//    List<String> stack,
    String techStack,
    ProjectStatus status,
    String authorName,
//    Integer totalPositions,
    Integer candidatesCount
){
}
