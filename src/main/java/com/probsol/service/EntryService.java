package com.probsol.service;

import com.probsol.dto.request.CreateEntryRequest;
import com.probsol.dto.request.UpdateEntryRequest;
import com.probsol.dto.request.UpdateStatusRequest;
import com.probsol.dto.response.EntriesListResponse;
import com.probsol.dto.response.EntryResponse;
import com.probsol.dto.response.EntryStatusResponse;
import com.probsol.dto.response.PaginationMeta;
import com.probsol.entity.Entry;
import com.probsol.entity.Tag;
import com.probsol.entity.User;
import com.probsol.exception.ResourceNotFoundException;
import com.probsol.repository.EntryRepository;
import com.probsol.repository.UserRepository;
import com.probsol.repository.specification.EntrySpecification;
import com.probsol.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;

@Service
public class EntryService {

    private final EntryRepository entryRepository;
    private final UserRepository userRepository;
    private final TagService tagService;

    public EntryService(EntryRepository entryRepository,
                        UserRepository userRepository,
                        TagService tagService) {
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
        this.tagService = tagService;
    }

    @Transactional
    public EntryResponse createEntry(UserPrincipal principal, CreateEntryRequest request) {
        User user = userRepository.getReferenceById(principal.getId());

        String type = request.type().trim().toLowerCase();
        String status = request.status();
        if (!StringUtils.hasText(status)) {
            status = "solution".equalsIgnoreCase(type) ? "SOLVED" : "OPEN";
        } else {
            status = status.trim().toUpperCase();
        }

        Entry entry = new Entry(
                user,
                type,
                status,
                request.title().trim(),
                request.description() != null ? request.description() : ""
        );

        if (request.tags() != null && !request.tags().isEmpty()) {
            Set<Tag> tags = tagService.getOrCreateTags(user, request.tags());
            entry.setTags(tags);
        }

        Entry saved = entryRepository.save(entry);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public EntryResponse getEntryById(UserPrincipal principal, String id) {
        Entry entry = entryRepository.findByIdAndUserIdAndDeletedAtIsNull(id, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));
        return mapToResponse(entry);
    }

    @Transactional(readOnly = true)
    public EntriesListResponse listEntries(
            UserPrincipal principal,
            String q,
            String type,
            String status,
            String tagsParam,
            String tagMode,
            String from,
            String to,
            String sort,
            int page,
            int limit) {

        int validPage = Math.max(1, page);
        int validLimit = Math.min(100, Math.max(1, limit));

        Sort sortObj = parseSort(sort);
        PageRequest pageRequest = PageRequest.of(validPage - 1, validLimit, sortObj);

        List<String> tagList = null;
        if (StringUtils.hasText(tagsParam)) {
            tagList = Arrays.stream(tagsParam.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        Instant fromDate = EntrySpecification.parseDate(from, false);
        Instant toDate = EntrySpecification.parseDate(to, true);

        Specification<Entry> spec = EntrySpecification.filterEntries(
                principal.getId(),
                q,
                type,
                status,
                tagList,
                tagMode,
                fromDate,
                toDate
        );

        Page<Entry> entryPage = entryRepository.findAll(spec, pageRequest);

        List<EntryResponse> items = entryPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PaginationMeta meta = new PaginationMeta(
                validPage,
                validLimit,
                entryPage.getTotalElements(),
                entryPage.getTotalPages(),
                entryPage.hasNext(),
                entryPage.hasPrevious()
        );

        return new EntriesListResponse(items, meta);
    }

    @Transactional
    public EntryResponse updateEntry(UserPrincipal principal, String id, UpdateEntryRequest request) {
        Entry entry = entryRepository.findByIdAndUserIdAndDeletedAtIsNull(id, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));

        if (StringUtils.hasText(request.title())) {
            entry.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            entry.setDescription(request.description());
        }
        if (StringUtils.hasText(request.type())) {
            entry.setType(request.type().trim().toLowerCase());
        }
        if (StringUtils.hasText(request.status())) {
            entry.setStatus(request.status().trim().toUpperCase());
        }
        if (request.tags() != null) {
            User user = userRepository.getReferenceById(principal.getId());
            Set<Tag> tags = tagService.getOrCreateTags(user, request.tags());
            entry.setTags(tags);
        }

        entry.setUpdatedAt(Instant.now());
        Entry saved = entryRepository.save(entry);
        return mapToResponse(saved);
    }

    @Transactional
    public EntryStatusResponse updateStatus(UserPrincipal principal, String id, UpdateStatusRequest request) {
        Entry entry = entryRepository.findByIdAndUserIdAndDeletedAtIsNull(id, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));

        entry.setStatus(request.status().trim().toUpperCase());
        entry.setUpdatedAt(Instant.now());
        Entry saved = entryRepository.save(entry);

        return new EntryStatusResponse(saved.getId(), saved.getStatus(), saved.getUpdatedAt());
    }

    @Transactional
    public void deleteEntry(UserPrincipal principal, String id) {
        Entry entry = entryRepository.findByIdAndUserIdAndDeletedAtIsNull(id, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));

        entry.setDeletedAt(Instant.now());
        entryRepository.save(entry);
    }

    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort.trim().toLowerCase()) {
            case "created_at:asc" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "title:asc" -> Sort.by(Sort.Direction.ASC, "title");
            case "relevance" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private EntryResponse mapToResponse(Entry entry) {
        List<String> tagNames = entry.getTags() != null
                ? entry.getTags().stream().map(Tag::getName).sorted().toList()
                : Collections.emptyList();

        return new EntryResponse(
                entry.getId(),
                entry.getType(),
                entry.getStatus(),
                entry.getTitle(),
                entry.getDescription(),
                tagNames,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
