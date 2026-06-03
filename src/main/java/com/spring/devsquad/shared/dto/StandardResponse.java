package com.spring.devsquad.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Template for API responses, following JSend pattern.
 * @author Filipe Martins
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StandardResponse<T> (
    String status,
    T data,
    String message
) {
    public static <T> StandardResponse<T> success(T data) {
        return new StandardResponse<>("success", data, null);
    }

    public static <T> StandardResponse<T> fail(String message) {
        return new StandardResponse<>("fail", null, message);
    }

    public static <T> StandardResponse<T> error(String message) {
        return new StandardResponse<>("error", null, message);
    }
}
