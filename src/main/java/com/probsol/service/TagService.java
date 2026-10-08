package com.probsol.service;

import com.probsol.dto.response.TagCountProjection;
import com.probsol.dto.response.TagResponse;
import com.probsol.entity.Tag;
import com.probsol.entity.User;
import com.probsol.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public Set<Tag> getOrCreateTags(User user, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }

        Set<Tag> tags = new HashSet<>();
        for (String rawName : tagNames) {
            if (rawName == null) continue;
            String normalized = rawName.trim().toLowerCase();
            if (normalized.isEmpty()) continue;

            Tag tag = tagRepository.findByUserIdAndName(user.getId(), normalized)
                    .orElseGet(() -> tagRepository.save(new Tag(user, normalized)));
            tags.add(tag);
        }
        return tags;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getUserTags(String userId) {
        List<TagCountProjection> projections = tagRepository.findTagsWithCountByUserId(userId);
        return projections.stream()
                .map(p -> new TagResponse(p.getId(), p.getName(), p.getCount() != null ? p.getCount() : 0L))
                .toList();
    }
}
