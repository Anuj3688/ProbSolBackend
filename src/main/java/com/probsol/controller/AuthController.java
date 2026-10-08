package com.probsol.controller;

import com.probsol.dto.request.LoginRequest;
import com.probsol.dto.request.RegisterRequest;
import com.probsol.dto.response.ApiResponse;
import com.probsol.dto.response.AuthResponse;
import com.probsol.dto.response.RefreshTokenResponse;
import com.probsol.dto.response.UserResponse;
import com.probsol.security.UserPrincipal;
import com.probsol.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, and session management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(authResponse));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT + refresh cookie")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue a fresh access token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        RefreshTokenResponse refreshResponse = authService.refreshToken(request, response);
        return ResponseEntity.ok(ApiResponse.success(refreshResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke session and clear refresh cookie")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.message("Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserResponse user = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
