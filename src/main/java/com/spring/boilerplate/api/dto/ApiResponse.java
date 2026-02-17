package com.spring.boilerplate.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Template for API responses, following JSend pattern.
 * @author Filipe Martins
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse <T> (
    String status,
    T data,
    String message
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data, null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>("fail", null, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("error", null, message);
    }
}
