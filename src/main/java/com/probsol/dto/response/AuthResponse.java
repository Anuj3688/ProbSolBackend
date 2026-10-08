package com.probsol.dto.response;

public record AuthResponse(
    UserResponse user,
    String accessToken
) {}
