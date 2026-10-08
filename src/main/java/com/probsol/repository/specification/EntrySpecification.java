package com.probsol.repository.specification;

import com.probsol.entity.Entry;
import com.probsol.entity.Tag;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class EntrySpecification {

    public static Specification<Entry> filterEntries(
            String userId,
            String q,
            String type,
            String status,
            List<String> tags,
            String tagMode,
            Instant fromDate,
            Instant toDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory user tenant isolation
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            // 2. Exclude soft-deleted items
            predicates.add(cb.isNull(root.get("deletedAt")));

            // 3. Full-text / partial search query across title & description
            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleLike, descLike));
            }

            // 4. Type filter (problem / solution)
            if (StringUtils.hasText(type)) {
                predicates.add(cb.equal(root.get("type"), type.trim().toLowerCase()));
            }

            // 5. Status filter (OPEN / SOLVED)
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status.trim().toUpperCase()));
            }

            // 6. Tags filter
            if (tags != null && !tags.isEmpty()) {
                List<String> normalizedTags = tags.stream()
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .filter(s -> !s.isEmpty())
                        .toList();

                if (!normalizedTags.isEmpty()) {
                    boolean matchAll = "all".equalsIgnoreCase(tagMode);
                    if (matchAll) {
                        // All tags must exist on the entry
                        for (String tag : normalizedTags) {
                            Subquery<Long> subquery = query.subquery(Long.class);
                            Root<Entry> subRoot = subquery.from(Entry.class);
                            Join<Entry, Tag> tagJoin = subRoot.join("tags");
                            subquery.select(cb.literal(1L))
                                    .where(
                                            cb.equal(subRoot.get("id"), root.get("id")),
                                            cb.equal(tagJoin.get("name"), tag)
                                    );
                            predicates.add(cb.exists(subquery));
                        }
                    } else {
                        // At least one of the tags must exist
                        Subquery<Long> subquery = query.subquery(Long.class);
                        Root<Entry> subRoot = subquery.from(Entry.class);
                        Join<Entry, Tag> tagJoin = subRoot.join("tags");
                        subquery.select(cb.literal(1L))
                                .where(
                                        cb.equal(subRoot.get("id"), root.get("id")),
                                        tagJoin.get("name").in(normalizedTags)
                                );
                        predicates.add(cb.exists(subquery));
                    }
                }
            }

            // 7. Date range filters
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Instant parseDate(String dateStr, boolean endOfDay) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return Instant.parse(dateStr.trim());
        } catch (Exception ignored) {
        }
        try {
            LocalDate localDate = LocalDate.parse(dateStr.trim());
            if (endOfDay) {
                return localDate.atTime(23, 59, 59, 999_999_999).atZone(ZoneOffset.UTC).toInstant();
            } else {
                return localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
