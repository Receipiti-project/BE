package com.receipiti.be.domain.expenditure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.category.repository.CategoryRepository;
import com.receipiti.be.domain.categoryhistory.dto.CategoryRecommendation;
import com.receipiti.be.domain.categoryhistory.enums.CategorySelectionSource;
import com.receipiti.be.domain.categoryhistory.enums.RecommendationReason;
import com.receipiti.be.domain.categoryhistory.service.CategorySelectionHistoryService;
import com.receipiti.be.domain.categoryhistory.service.PersonalizedCategoryService;
import com.receipiti.be.domain.expenditure.dto.request.ExpenditureCreateRequest;
import com.receipiti.be.domain.expenditure.dto.request.ExpenditureUpdateRequest;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureCreateResponse;
import com.receipiti.be.domain.expenditure.entity.Expenditure;
import com.receipiti.be.domain.expenditure.enums.CategoryClassificationType;
import com.receipiti.be.domain.expenditure.enums.Currency;
import com.receipiti.be.domain.expenditure.enums.InputType;
import com.receipiti.be.domain.expenditure.repository.ExpenditureRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import com.receipiti.be.domain.store.repository.StoreRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpenditureServiceTest {

    @Mock
    private ExpenditureRepository expenditureRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private CategorySelectionHistoryService categorySelectionHistoryService;
    @Mock
    private PersonalizedCategoryService personalizedCategoryService;
    @Mock
    private NaverOcrHandler naverOcrHandler;
    @InjectMocks
    private ExpenditureService expenditureService;

    private Member member;
    private Category category;
    private Store store;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).build();
        category = Category.builder().id(2L).build();
        store = Store.builder().id(3L).name("스타벅스 강남점").build();
    }

    @Test
    void 지출을_생성하면_카테고리_선택_이력을_저장한다() {
        ExpenditureCreateRequest request = org.mockito.Mockito.mock(ExpenditureCreateRequest.class);
        LocalDateTime expenditureDate = LocalDateTime.of(2026, 8, 18, 12, 0);
        when(request.getCategoryId()).thenReturn(category.getId());
        when(request.getStoreName()).thenReturn(store.getName());
        when(request.getAmount()).thenReturn(5_000L);
        when(request.getExpenditureDate()).thenReturn(expenditureDate);
        when(categoryRepository.findAccessibleCategory(category.getId(), member))
                .thenReturn(Optional.of(category));
        when(storeRepository.findByName(store.getName())).thenReturn(Optional.of(store));
        when(expenditureRepository.save(org.mockito.ArgumentMatchers.any(Expenditure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExpenditureCreateResponse response = expenditureService.createExpenditure(member, request);

        verify(categorySelectionHistoryService).recordSelection(
                member,
                category,
                store,
                CategorySelectionSource.CREATE
        );
        verify(personalizedCategoryService, never()).recommend(member, store);
        assertThat(response.getClassificationType())
                .isEqualTo(CategoryClassificationType.USER_SELECTED);
    }

    @Test
    void 기준을_충족한_개인화_추천을_자동_적용하고_학습_이력은_저장하지_않는다() {
        ExpenditureCreateRequest request = createAutomaticRequest();
        Category personalizedCategory = Category.builder().id(5L).name("카페").build();
        CategoryRecommendation recommendation = new CategoryRecommendation(
                personalizedCategory,
                3,
                180.0,
                0.75,
                true,
                RecommendationReason.SAME_BRAND
        );
        when(storeRepository.findByName(store.getName())).thenReturn(Optional.of(store));
        when(personalizedCategoryService.recommend(member, store))
                .thenReturn(Optional.of(recommendation));
        when(expenditureRepository.save(org.mockito.ArgumentMatchers.any(Expenditure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExpenditureCreateResponse response = expenditureService.createExpenditure(member, request);

        assertThat(response.getCategoryId()).isEqualTo(personalizedCategory.getId());
        assertThat(response.getCategoryName()).isEqualTo("카페");
        assertThat(response.getClassificationType())
                .isEqualTo(CategoryClassificationType.PERSONALIZED_AUTO);
        assertThat(response.getConfidence()).isEqualTo(0.75);
        assertThat(response.getRecommendationReason()).isEqualTo(RecommendationReason.SAME_BRAND);
        verify(categoryRepository, never()).findAccessibleCategory(request.getDefaultCategoryId(), member);
        verify(categorySelectionHistoryService, never()).recordSelection(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void 개인화_추천의_신뢰도가_부족하면_시스템_기본_카테고리를_적용한다() {
        ExpenditureCreateRequest request = createAutomaticRequest();
        Category defaultCategory = Category.builder().id(6L).name("식비").build();
        CategoryRecommendation recommendation = new CategoryRecommendation(
                category,
                2,
                120.0,
                1.0,
                false,
                RecommendationReason.SAME_BRAND
        );
        when(storeRepository.findByName(store.getName())).thenReturn(Optional.of(store));
        when(personalizedCategoryService.recommend(member, store))
                .thenReturn(Optional.of(recommendation));
        when(categoryRepository.findAccessibleCategory(request.getDefaultCategoryId(), member))
                .thenReturn(Optional.of(defaultCategory));
        when(expenditureRepository.save(org.mockito.ArgumentMatchers.any(Expenditure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExpenditureCreateResponse response = expenditureService.createExpenditure(member, request);

        assertThat(response.getCategoryId()).isEqualTo(defaultCategory.getId());
        assertThat(response.getClassificationType())
                .isEqualTo(CategoryClassificationType.SYSTEM_DEFAULT);
        assertThat(response.getConfidence()).isNull();
        assertThat(response.getRecommendationReason()).isNull();
        verify(categorySelectionHistoryService, never()).recordSelection(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void 개인화_이력이_없으면_시스템_기본_카테고리를_적용한다() {
        ExpenditureCreateRequest request = createAutomaticRequest();
        Category defaultCategory = Category.builder().id(6L).name("식비").build();
        when(storeRepository.findByName(store.getName())).thenReturn(Optional.of(store));
        when(personalizedCategoryService.recommend(member, store)).thenReturn(Optional.empty());
        when(categoryRepository.findAccessibleCategory(request.getDefaultCategoryId(), member))
                .thenReturn(Optional.of(defaultCategory));
        when(expenditureRepository.save(org.mockito.ArgumentMatchers.any(Expenditure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExpenditureCreateResponse response = expenditureService.createExpenditure(member, request);

        assertThat(response.getCategoryId()).isEqualTo(defaultCategory.getId());
        assertThat(response.getClassificationType())
                .isEqualTo(CategoryClassificationType.SYSTEM_DEFAULT);
    }

    @Test
    void 카테고리를_변경하면_수정_이력을_저장한다() {
        Category changedCategory = Category.builder().id(4L).build();
        Expenditure expenditure = createExpenditure();
        ExpenditureUpdateRequest request = org.mockito.Mockito.mock(ExpenditureUpdateRequest.class);
        when(request.getCategoryId()).thenReturn(changedCategory.getId());
        when(expenditureRepository.findByIdAndMember(expenditure.getId(), member))
                .thenReturn(Optional.of(expenditure));
        when(categoryRepository.findAccessibleCategory(changedCategory.getId(), member))
                .thenReturn(Optional.of(changedCategory));

        expenditureService.updateExpenditure(member, expenditure.getId(), request);

        verify(categorySelectionHistoryService).recordSelection(
                member,
                changedCategory,
                store,
                CategorySelectionSource.UPDATE
        );
    }

    @Test
    void 카테고리가_같으면_수정_이력을_저장하지_않는다() {
        Expenditure expenditure = createExpenditure();
        ExpenditureUpdateRequest request = org.mockito.Mockito.mock(ExpenditureUpdateRequest.class);
        when(request.getCategoryId()).thenReturn(category.getId());
        when(expenditureRepository.findByIdAndMember(expenditure.getId(), member))
                .thenReturn(Optional.of(expenditure));

        expenditureService.updateExpenditure(member, expenditure.getId(), request);

        verify(categorySelectionHistoryService, never()).recordSelection(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private Expenditure createExpenditure() {
        return Expenditure.builder()
                .id(10L)
                .member(member)
                .category(category)
                .store(store)
                .amount(5_000L)
                .inputType(InputType.MANUAL)
                .expenditureDate(LocalDateTime.of(2026, 8, 18, 12, 0))
                .currency(Currency.KRW)
                .build();
    }

    private ExpenditureCreateRequest createAutomaticRequest() {
        ExpenditureCreateRequest request = new ExpenditureCreateRequest();
        request.setDefaultCategoryId(6L);
        request.setStoreName(store.getName());
        request.setAmount(5_000L);
        request.setExpenditureDate(LocalDateTime.of(2026, 8, 18, 12, 0));
        return request;
    }
}
