package com.receipiti.be.domain.report.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReportResponse(
        String targetMonth,
        Long totalAmount,
        Integer transactionCount,
        boolean anomalyDetected,
        String anomalyReason,
        TimeAnalysis frequentSpendingTime,
        DayAnalysis frequentSpendingDay,
        CategoryAnalysis topCategory,
        List<String> spendingPatternInsights,
        String summary
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimeAnalysis(
            String timeRange,
            Integer transactionCount,
            Long amount,
            String description
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DayAnalysis(
            String dayOfWeek,
            Integer transactionCount,
            Long amount,
            String description
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CategoryAnalysis(
            String categoryName,
            Long amount,
            Double percentage,
            String description
    ) {
    }
}
