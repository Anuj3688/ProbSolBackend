package com.probsol.controller;

import com.probsol.dto.response.ApiResponse;
import com.probsol.dto.response.TagResponse;
import com.probsol.security.UserPrincipal;
import com.probsol.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@Tag(name = "Tags", description = "Endpoints for managing user tags and tag counts")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "Get user tags with active entry counts")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getTags(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<TagResponse> tags = tagService.getUserTags(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(tags));
    }
}
