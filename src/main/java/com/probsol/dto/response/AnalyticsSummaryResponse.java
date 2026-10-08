package com.probsol.dto.response;

import java.util.List;

public record AnalyticsSummaryResponse(
    long totalEntries,
    ProblemStats problems,
    SolutionStats solutions,
    double solveRatio,
    List<TopTag> topTags
) {
    public record ProblemStats(long total, long open, long solved) {}
    public record SolutionStats(long total, long solved) {}
    public record TopTag(String name, long count) {}
}
