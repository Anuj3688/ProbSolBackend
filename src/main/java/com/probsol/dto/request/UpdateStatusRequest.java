package com.probsol.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateStatusRequest(
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(OPEN|SOLVED)$", message = "Status must be 'OPEN' or 'SOLVED'")
    String status
) {}
