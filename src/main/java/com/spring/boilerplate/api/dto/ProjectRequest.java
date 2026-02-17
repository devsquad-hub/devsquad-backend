package com.spring.boilerplate.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProjectRequest (
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String techStack,
    @NotBlank String difficulty,
    @NotNull @Min(1) Integer totalPositions
){
}
