package com.probsol.dto.response;

public record PaginationMeta(
    int page,
    int limit,
    long totalItems,
    int totalPages,
    boolean hasNextPage,
    boolean hasPrevPage
) {}
