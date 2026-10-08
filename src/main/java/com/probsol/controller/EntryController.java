package com.probsol.controller;

import com.probsol.dto.request.CreateEntryRequest;
import com.probsol.dto.request.UpdateEntryRequest;
import com.probsol.dto.request.UpdateStatusRequest;
import com.probsol.dto.response.ApiResponse;
import com.probsol.dto.response.EntriesListResponse;
import com.probsol.dto.response.EntryResponse;
import com.probsol.dto.response.EntryStatusResponse;
import com.probsol.security.UserPrincipal;
import com.probsol.service.EntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/entries")
@Tag(name = "Entries", description = "Endpoints for managing problem and solution notes")
public class EntryController {

    private final EntryService entryService;

    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    @PostMapping
    @Operation(summary = "Create a new problem or solution entry")
    public ResponseEntity<ApiResponse<EntryResponse>> createEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateEntryRequest request) {
        EntryResponse response = entryService.createEntry(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Search, filter, and paginate entries")
    public ResponseEntity<ApiResponse<EntriesListResponse>> listEntries(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tags,
            @RequestParam(name = "tag_mode", required = false, defaultValue = "any") String tagMode,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "created_at:desc") String sort,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {

        EntriesListResponse response = entryService.listEntries(
                principal, q, type, status, tags, tagMode, from, to, sort, page, limit
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single entry by ID")
    public ResponseEntity<ApiResponse<EntryResponse>> getEntryById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        EntryResponse response = entryService.getEntryById(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update entry fields")
    public ResponseEntity<ApiResponse<EntryResponse>> updateEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody UpdateEntryRequest request) {
        EntryResponse response = entryService.updateEntry(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update status of an entry (OPEN or SOLVED)")
    public ResponseEntity<ApiResponse<EntryStatusResponse>> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest request) {
        EntryStatusResponse response = entryService.updateStatus(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete an entry")
    public ResponseEntity<ApiResponse<Void>> deleteEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        entryService.deleteEntry(principal, id);
        return ResponseEntity.ok(ApiResponse.message("Entry removed successfully"));
    }
}
