package com.probsol.dto.response;

import java.time.Instant;
import java.util.List;

public record EntryResponse(
    String id,
    String type,
    String status,
    String title,
    String description,
    List<String> tags,
    Instant createdAt,
    Instant updatedAt
) {}
