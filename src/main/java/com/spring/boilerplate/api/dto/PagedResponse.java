package com.spring.boilerplate.api.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record PagedResponse<T> (
    int page,
    int size,
    boolean isLast,
    long totalElements,
    int totalPages,
    List<T> content
){
}
