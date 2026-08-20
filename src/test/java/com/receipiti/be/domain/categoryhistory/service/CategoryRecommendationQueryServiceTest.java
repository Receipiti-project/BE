package com.receipiti.be.domain.categoryhistory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.category.enums.CategoryType;
import com.receipiti.be.domain.categoryhistory.dto.CategoryRecommendation;
import com.receipiti.be.domain.categoryhistory.dto.CategoryRecommendationResponse;
import com.receipiti.be.domain.categoryhistory.enums.RecommendationReason;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import com.receipiti.be.domain.store.repository.StoreRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryRecommendationQueryServiceTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private PersonalizedCategoryService personalizedCategoryService;
    @InjectMocks
    private CategoryRecommendationQueryService categoryRecommendationQueryService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).build();
    }

    @Test
    void 저장되지_않은_가맹점도_브랜드와_업종으로_추천을_조회한다() {
        Category customCategory = Category.builder()
                .id(2L)
                .member(member)
                .name("카페")
                .categoryType(CategoryType.CUSTOM)
                .build();
        CategoryRecommendation recommendation = new CategoryRecommendation(
                customCategory,
                3,
                180.0,
                0.8,
                true,
                RecommendationReason.SAME_BRAND
        );
        when(storeRepository.findByName("스타벅스 홍대점")).thenReturn(Optional.empty());
        ArgumentCaptor<Store> storeCaptor = ArgumentCaptor.forClass(Store.class);
        when(personalizedCategoryService.recommend(
                org.mockito.ArgumentMatchers.eq(member),
                storeCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("카페")
        )).thenReturn(Optional.of(recommendation));

        CategoryRecommendationResponse response = categoryRecommendationQueryService
                .getRecommendation(member, "  스타벅스 홍대점  ", "  카페  ");

        assertThat(storeCaptor.getValue().getName()).isEqualTo("스타벅스 홍대점");
        assertThat(storeCaptor.getValue().getBizCategory()).isEqualTo("카페");
        assertThat(response.categoryId()).isEqualTo(customCategory.getId());
        assertThat(response.categoryName()).isEqualTo("카페");
        assertThat(response.custom()).isTrue();
        assertThat(response.autoApplicable()).isTrue();
        assertThat(response.reason()).isEqualTo(RecommendationReason.SAME_BRAND);
    }

    @Test
    void 추천_이력이_없으면_빈_추천_응답을_반환한다() {
        Store store = Store.builder().id(3L).name("새 가맹점").build();
        when(storeRepository.findByName(store.getName())).thenReturn(Optional.of(store));
        when(personalizedCategoryService.recommend(member, store, null))
                .thenReturn(Optional.empty());

        CategoryRecommendationResponse response = categoryRecommendationQueryService
                .getRecommendation(member, store.getName(), null);

        assertThat(response.categoryId()).isNull();
        assertThat(response.matchedCount()).isZero();
        assertThat(response.confidence()).isZero();
        assertThat(response.autoApplicable()).isFalse();
        assertThat(response.reason()).isNull();
        verify(personalizedCategoryService).recommend(member, store, null);
    }
}
