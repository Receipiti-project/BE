package com.receipiti.be.domain.categoryhistory.dto;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.categoryhistory.enums.RecommendationReason;

public record CategoryRecommendation(
        Category category,
        int matchedCount,
        double score,
        double confidence,
        boolean autoApplicable,
        RecommendationReason reason
) {
}
