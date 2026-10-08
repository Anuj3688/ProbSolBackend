package com.probsol.dto.response;

import java.util.List;

public record EntriesListResponse(
    List<EntryResponse> items,
    PaginationMeta pagination
) {}
