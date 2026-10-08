package com.probsol.controller;

import com.probsol.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health", description = "Public health and status check endpoints")
public class HealthController {

    @GetMapping("/")
    @Operation(summary = "Root welcome endpoint")
    public ResponseEntity<ApiResponse<Map<String, String>>> root() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "UP",
                "service", "ProbSol Backend API",
                "version", "1.0.0"
        )));
    }

    @GetMapping("/api/v1/health")
    @Operation(summary = "Health status endpoint for uptime monitors and Render")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "UP"
        )));
    }
}
