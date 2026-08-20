package com.receipiti.be.domain.categoryhistory.service;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.categoryhistory.dto.CategoryRecommendation;
import com.receipiti.be.domain.categoryhistory.entity.CategorySelectionHistory;
import com.receipiti.be.domain.categoryhistory.enums.RecommendationReason;
import com.receipiti.be.domain.categoryhistory.repository.CategorySelectionHistoryRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalizedCategoryService {

    private static final int SAME_STORE_SCORE = 100;
    private static final int SAME_BRAND_SCORE = 50;
    private static final int SAME_BUSINESS_CATEGORY_SCORE = 20;
    private static final int RECENT_30_DAYS_BONUS = 10;
    private static final int RECENT_90_DAYS_BONUS = 5;
    private static final int MINIMUM_AUTO_APPLY_COUNT = 3;
    private static final double MINIMUM_AUTO_APPLY_CONFIDENCE = 0.75;

    private final CategorySelectionHistoryRepository categorySelectionHistoryRepository;
    private final MerchantNameNormalizer merchantNameNormalizer;

    public Optional<CategoryRecommendation> recommend(Member member, Store store) {
        return recommend(member, store, store.getBizCategory());
    }

    public Optional<CategoryRecommendation> recommend(
            Member member,
            Store store,
            String businessCategory
    ) {
        RecommendationCandidates candidates = findCandidates(member, store, businessCategory);
        if (candidates.histories().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(calculateRecommendation(candidates));
    }

    private RecommendationCandidates findCandidates(
            Member member,
            Store store,
            String businessCategory
    ) {
        if (store.getId() != null) {
            List<CategorySelectionHistory> sameStoreHistories = categorySelectionHistoryRepository
                    .findAllByMemberAndStoreOrderByCreatedAtDesc(member, store);
            if (!sameStoreHistories.isEmpty()) {
                return new RecommendationCandidates(
                        sameStoreHistories,
                        SAME_STORE_SCORE,
                        RecommendationReason.SAME_STORE
                );
            }
        }

        String normalizedBrandName = merchantNameNormalizer.normalizeBrandName(store.getName());
        if (!normalizedBrandName.isBlank()) {
            List<CategorySelectionHistory> sameBrandHistories = categorySelectionHistoryRepository
                    .findAllByMemberAndNormalizedBrandNameOrderByCreatedAtDesc(member, normalizedBrandName);
            if (!sameBrandHistories.isEmpty()) {
                return new RecommendationCandidates(
                        sameBrandHistories,
                        SAME_BRAND_SCORE,
                        RecommendationReason.SAME_BRAND
                );
            }
        }

        if (businessCategory != null && !businessCategory.isBlank()) {
            List<CategorySelectionHistory> sameBusinessHistories = categorySelectionHistoryRepository
                    .findAllByMemberAndBusinessCategoryOrderByCreatedAtDesc(
                            member,
                            businessCategory
                    );
            if (!sameBusinessHistories.isEmpty()) {
                return new RecommendationCandidates(
                        sameBusinessHistories,
                        SAME_BUSINESS_CATEGORY_SCORE,
                        RecommendationReason.SAME_BUSINESS_CATEGORY
                );
            }
        }

        return new RecommendationCandidates(List.of(), 0, null);
    }

    private CategoryRecommendation calculateRecommendation(RecommendationCandidates candidates) {
        LocalDateTime now = LocalDateTime.now();
        Map<Category, CategoryScore> scores = new HashMap<>();

        for (CategorySelectionHistory history : candidates.histories()) {
            CategoryScore score = scores.computeIfAbsent(
                    history.getCategory(),
                    CategoryScore::new
            );
            score.add(candidates.baseScore() + recentBonus(history.getCreatedAt(), now), history.getCreatedAt());
        }

        CategoryScore top = scores.values().stream()
                .max(Comparator.comparingDouble(CategoryScore::score)
                        .thenComparing(
                                CategoryScore::latestSelectionAt,
                                Comparator.nullsFirst(Comparator.naturalOrder())
                        ))
                .orElseThrow();
        double totalScore = scores.values().stream().mapToDouble(CategoryScore::score).sum();
        double confidence = top.score() / totalScore;
        boolean autoApplicable = top.count() >= MINIMUM_AUTO_APPLY_COUNT
                && confidence >= MINIMUM_AUTO_APPLY_CONFIDENCE;

        return new CategoryRecommendation(
                top.category(),
                top.count(),
                top.score(),
                confidence,
                autoApplicable,
                candidates.reason()
        );
    }

    private int recentBonus(LocalDateTime selectedAt, LocalDateTime now) {
        if (selectedAt == null) {
            return 0;
        }
        if (!selectedAt.isBefore(now.minusDays(30))) {
            return RECENT_30_DAYS_BONUS;
        }
        if (!selectedAt.isBefore(now.minusDays(90))) {
            return RECENT_90_DAYS_BONUS;
        }
        return 0;
    }

    private record RecommendationCandidates(
            List<CategorySelectionHistory> histories,
            int baseScore,
            RecommendationReason reason
    ) {
    }

    private static final class CategoryScore {

        private final Category category;
        private int count;
        private double score;
        private LocalDateTime latestSelectionAt;

        private CategoryScore(Category category) {
            this.category = category;
        }

        private void add(double addedScore, LocalDateTime selectedAt) {
            count++;
            score += addedScore;
            if (selectedAt != null
                    && (latestSelectionAt == null || selectedAt.isAfter(latestSelectionAt))) {
                latestSelectionAt = selectedAt;
            }
        }

        private Category category() {
            return category;
        }

        private int count() {
            return count;
        }

        private double score() {
            return score;
        }

        private LocalDateTime latestSelectionAt() {
            return latestSelectionAt;
        }
    }
}
