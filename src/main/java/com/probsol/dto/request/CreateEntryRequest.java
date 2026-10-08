package com.probsol.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateEntryRequest(
    @NotBlank(message = "Type is required")
    @Pattern(regexp = "^(problem|solution)$", message = "Type must be 'problem' or 'solution'")
    String type,

    @Pattern(regexp = "^(OPEN|SOLVED)$", message = "Status must be 'OPEN' or 'SOLVED'")
    String status,

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 300, message = "Title must be between 1 and 300 characters")
    String title,

    @Size(max = 10000, message = "Description cannot exceed 10000 characters")
    String description,

    @Size(max = 10, message = "Maximum 10 tags allowed")
    List<@Size(max = 50, message = "Tag length cannot exceed 50 characters") String> tags
) {}
