package com.probsol.controller;

import com.probsol.dto.response.AnalyticsSummaryResponse;
import com.probsol.dto.response.ApiResponse;
import com.probsol.security.UserPrincipal;
import com.probsol.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> getSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        AnalyticsSummaryResponse summary = analyticsService.getSummary(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
