package com.probsol.dto.response;

import java.time.Instant;

public record EntryStatusResponse(
    String id,
    String status,
    Instant updatedAt
) {}
