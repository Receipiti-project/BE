package com.receipiti.be.domain.categoryhistory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.categoryhistory.dto.CategoryRecommendation;
import com.receipiti.be.domain.categoryhistory.entity.CategorySelectionHistory;
import com.receipiti.be.domain.categoryhistory.enums.RecommendationReason;
import com.receipiti.be.domain.categoryhistory.repository.CategorySelectionHistoryRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PersonalizedCategoryServiceTest {

    @Mock
    private CategorySelectionHistoryRepository categorySelectionHistoryRepository;
    @Mock
    private MerchantNameNormalizer merchantNameNormalizer;
    @InjectMocks
    private PersonalizedCategoryService personalizedCategoryService;

    private Member member;
    private Store store;
    private Category cafe;
    private Category food;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).build();
        store = Store.builder()
                .id(2L)
                .name("스타벅스 홍대점")
                .bizCategory("카페")
                .build();
        cafe = Category.builder().id(3L).name("카페").build();
        food = Category.builder().id(4L).name("식비").build();
    }

    @Test
    void 동일_가맹점_이력이_있으면_브랜드와_업종_이력을_조회하지_않는다() {
        when(categorySelectionHistoryRepository
                .findAllByMemberAndStoreOrderByCreatedAtDesc(member, store))
                .thenReturn(List.of(history(cafe, LocalDateTime.now().minusDays(5))));

        CategoryRecommendation recommendation = personalizedCategoryService
                .recommend(member, store)
                .orElseThrow();

        assertThat(recommendation.category()).isEqualTo(cafe);
        assertThat(recommendation.reason()).isEqualTo(RecommendationReason.SAME_STORE);
        verify(merchantNameNormalizer, never()).normalizeBrandName(store.getName());
        verify(categorySelectionHistoryRepository, never())
                .findAllByMemberAndBusinessCategoryOrderByCreatedAtDesc(member, "카페");
    }

    @Test
    void 동일_브랜드에서_세_번_선택하고_신뢰도가_75퍼센트면_자동_적용한다() {
        String normalizedBrandName = "스타벅스";
        LocalDateTime recent = LocalDateTime.now().minusDays(5);
        when(categorySelectionHistoryRepository
                .findAllByMemberAndStoreOrderByCreatedAtDesc(member, store))
                .thenReturn(List.of());
        when(merchantNameNormalizer.normalizeBrandName(store.getName()))
                .thenReturn(normalizedBrandName);
        when(categorySelectionHistoryRepository
                .findAllByMemberAndNormalizedBrandNameOrderByCreatedAtDesc(member, normalizedBrandName))
                .thenReturn(List.of(
                        history(cafe, recent),
                        history(cafe, recent.minusDays(1)),
                        history(cafe, recent.minusDays(2)),
                        history(food, recent.minusDays(3))
                ));

        CategoryRecommendation recommendation = personalizedCategoryService
                .recommend(member, store)
                .orElseThrow();

        assertThat(recommendation.category()).isEqualTo(cafe);
        assertThat(recommendation.matchedCount()).isEqualTo(3);
        assertThat(recommendation.score()).isEqualTo(180.0);
        assertThat(recommendation.confidence()).isEqualTo(0.75);
        assertThat(recommendation.autoApplicable()).isTrue();
        assertThat(recommendation.reason()).isEqualTo(RecommendationReason.SAME_BRAND);
    }

    @Test
    void 브랜드_이력이_없으면_동일_업종_선택_빈도로_추천한다() {
        when(categorySelectionHistoryRepository
                .findAllByMemberAndStoreOrderByCreatedAtDesc(member, store))
                .thenReturn(List.of());
        when(merchantNameNormalizer.normalizeBrandName(store.getName())).thenReturn("스타벅스");
        when(categorySelectionHistoryRepository
                .findAllByMemberAndNormalizedBrandNameOrderByCreatedAtDesc(member, "스타벅스"))
                .thenReturn(List.of());
        when(categorySelectionHistoryRepository
                .findAllByMemberAndBusinessCategoryOrderByCreatedAtDesc(member, "카페"))
                .thenReturn(List.of(
                        history(cafe, LocalDateTime.now().minusDays(10)),
                        history(cafe, LocalDateTime.now().minusDays(20))
                ));

        CategoryRecommendation recommendation = personalizedCategoryService
                .recommend(member, store)
                .orElseThrow();

        assertThat(recommendation.category()).isEqualTo(cafe);
        assertThat(recommendation.matchedCount()).isEqualTo(2);
        assertThat(recommendation.confidence()).isEqualTo(1.0);
        assertThat(recommendation.autoApplicable()).isFalse();
        assertThat(recommendation.reason())
                .isEqualTo(RecommendationReason.SAME_BUSINESS_CATEGORY);
    }

    @Test
    void 최근_선택에_가산점을_부여한다() {
        when(categorySelectionHistoryRepository
                .findAllByMemberAndStoreOrderByCreatedAtDesc(member, store))
                .thenReturn(List.of(
                        history(cafe, LocalDateTime.now().minusDays(10)),
                        history(food, LocalDateTime.now().minusDays(100))
                ));

        CategoryRecommendation recommendation = personalizedCategoryService
                .recommend(member, store)
                .orElseThrow();

        assertThat(recommendation.category()).isEqualTo(cafe);
        assertThat(recommendation.score()).isEqualTo(110.0);
        assertThat(recommendation.confidence()).isEqualTo(110.0 / 210.0);
    }

    @Test
    void 개인화_이력이_없으면_추천을_반환하지_않는다() {
        when(categorySelectionHistoryRepository
                .findAllByMemberAndStoreOrderByCreatedAtDesc(member, store))
                .thenReturn(List.of());
        when(merchantNameNormalizer.normalizeBrandName(store.getName())).thenReturn("스타벅스");
        when(categorySelectionHistoryRepository
                .findAllByMemberAndNormalizedBrandNameOrderByCreatedAtDesc(member, "스타벅스"))
                .thenReturn(List.of());
        when(categorySelectionHistoryRepository
                .findAllByMemberAndBusinessCategoryOrderByCreatedAtDesc(member, "카페"))
                .thenReturn(List.of());

        Optional<CategoryRecommendation> recommendation =
                personalizedCategoryService.recommend(member, store);

        assertThat(recommendation).isEmpty();
    }

    @Test
    void 저장되지_않은_신규_가맹점은_동일_가맹점_조회를_건너뛴다() {
        Store newStore = Store.builder()
                .name("스타벅스 신규점")
                .bizCategory("카페")
                .build();
        when(merchantNameNormalizer.normalizeBrandName(newStore.getName())).thenReturn("스타벅스");
        when(categorySelectionHistoryRepository
                .findAllByMemberAndNormalizedBrandNameOrderByCreatedAtDesc(member, "스타벅스"))
                .thenReturn(List.of(history(cafe, LocalDateTime.now().minusDays(10))));

        CategoryRecommendation recommendation = personalizedCategoryService
                .recommend(member, newStore)
                .orElseThrow();

        assertThat(recommendation.reason()).isEqualTo(RecommendationReason.SAME_BRAND);
        verify(categorySelectionHistoryRepository, never())
                .findAllByMemberAndStoreOrderByCreatedAtDesc(member, newStore);
    }

    private CategorySelectionHistory history(Category category, LocalDateTime selectedAt) {
        CategorySelectionHistory history = CategorySelectionHistory.builder()
                .member(member)
                .category(category)
                .store(store)
                .storeName(store.getName())
                .normalizedStoreName("스타벅스홍대점")
                .normalizedBrandName("스타벅스")
                .businessCategory("카페")
                .build();
        ReflectionTestUtils.setField(history, "createdAt", selectedAt);
        return history;
    }
}
