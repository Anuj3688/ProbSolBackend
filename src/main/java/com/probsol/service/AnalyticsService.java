package com.probsol.service;

import com.probsol.dto.response.AnalyticsSummaryResponse;
import com.probsol.dto.response.TagResponse;
import com.probsol.repository.EntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AnalyticsService {

    private final EntryRepository entryRepository;
    private final TagService tagService;

    public AnalyticsService(EntryRepository entryRepository, TagService tagService) {
        this.entryRepository = entryRepository;
        this.tagService = tagService;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(String userId) {
        long totalEntries = entryRepository.countByUserIdAndDeletedAtIsNull(userId);

        long problemTotal = entryRepository.countByUserIdAndTypeAndDeletedAtIsNull(userId, "problem");
        long problemOpen = entryRepository.countByUserIdAndTypeAndStatusAndDeletedAtIsNull(userId, "problem", "OPEN");
        long problemSolved = entryRepository.countByUserIdAndTypeAndStatusAndDeletedAtIsNull(userId, "problem", "SOLVED");

        long solutionTotal = entryRepository.countByUserIdAndTypeAndDeletedAtIsNull(userId, "solution");
        long solutionSolved = entryRepository.countByUserIdAndTypeAndStatusAndDeletedAtIsNull(userId, "solution", "SOLVED");

        double solveRatio = 0.0;
        if (problemTotal > 0) {
            double ratio = (double) problemSolved / problemTotal;
            solveRatio = BigDecimal.valueOf(ratio)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        List<TagResponse> tags = tagService.getUserTags(userId);
        List<AnalyticsSummaryResponse.TopTag> topTags = tags.stream()
                .filter(t -> t.count() > 0)
                .limit(5)
                .map(t -> new AnalyticsSummaryResponse.TopTag(t.name(), t.count()))
                .toList();

        return new AnalyticsSummaryResponse(
                totalEntries,
                new AnalyticsSummaryResponse.ProblemStats(problemTotal, problemOpen, problemSolved),
                new AnalyticsSummaryResponse.SolutionStats(solutionTotal, solutionSolved),
                solveRatio,
                topTags
        );
    }
}
