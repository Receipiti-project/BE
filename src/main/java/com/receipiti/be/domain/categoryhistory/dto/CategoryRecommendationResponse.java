package com.receipiti.be.domain.categoryhistory.dto;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.category.enums.CategoryType;
import com.receipiti.be.domain.categoryhistory.enums.RecommendationReason;
import java.util.Optional;

public record CategoryRecommendationResponse(
        Long categoryId,
        String categoryName,
        String categoryType,
        boolean custom,
        int matchedCount,
        double score,
        double confidence,
        boolean autoApplicable,
        RecommendationReason reason
) {

    public static CategoryRecommendationResponse from(
            Optional<CategoryRecommendation> recommendation
    ) {
        return recommendation
                .map(CategoryRecommendationResponse::from)
                .orElseGet(CategoryRecommendationResponse::empty);
    }

    private static CategoryRecommendationResponse from(CategoryRecommendation recommendation) {
        Category category = recommendation.category();
        return new CategoryRecommendationResponse(
                category.getId(),
                category.getName(),
                category.getCategoryType().name(),
                category.getCategoryType() == CategoryType.CUSTOM,
                recommendation.matchedCount(),
                recommendation.score(),
                recommendation.confidence(),
                recommendation.autoApplicable(),
                recommendation.reason()
        );
    }

    private static CategoryRecommendationResponse empty() {
        return new CategoryRecommendationResponse(
                null,
                null,
                null,
                false,
                0,
                0.0,
                0.0,
                false,
                null
        );
    }
}
