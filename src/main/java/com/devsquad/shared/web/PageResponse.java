package com.devsquad.shared.web;

import java.util.List;

public record PageResponse<T>(List<T> items, int page, int size, long totalItems) {
    public static <T> PageResponse<T> singlePage(List<T> items) {
        return new PageResponse<>(items, 0, items.size(), items.size());
    }
}
