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
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureDetailResponse;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureElement;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureListResponse;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureUpdateResponse;
import com.receipiti.be.domain.expenditure.entity.Expenditure;
import com.receipiti.be.domain.expenditure.enums.CategoryClassificationType;
import com.receipiti.be.domain.expenditure.enums.Currency;
import com.receipiti.be.domain.expenditure.enums.InputType;
import com.receipiti.be.domain.expenditure.repository.ExpenditureRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import com.receipiti.be.domain.store.repository.StoreRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    void 가맹점명과_업종명의_공백을_정규화한_후_저장한다() {
        ExpenditureCreateRequest request = new ExpenditureCreateRequest();
        request.setCategoryId(category.getId());
        request.setStoreName("  스타벅스 강남점  ");
        request.setBusinessCategory("  카페  ");
        request.setAmount(5_000L);
        request.setExpenditureDate(LocalDateTime.of(2026, 8, 18, 12, 0));
        when(categoryRepository.findAccessibleCategory(category.getId(), member))
                .thenReturn(Optional.of(category));
        when(storeRepository.findByName("스타벅스 강남점")).thenReturn(Optional.empty());
        when(storeRepository.save(org.mockito.ArgumentMatchers.any(Store.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(expenditureRepository.save(org.mockito.ArgumentMatchers.any(Expenditure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        expenditureService.createExpenditure(member, request);

        ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
        verify(storeRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("스타벅스 강남점");
        assertThat(captor.getValue().getBizCategory()).isEqualTo("카페");
    }

    @Test
    void 기존_업종명이_공백이면_새_업종명으로_보완한다() {
        Store storeWithBlankBusinessCategory = Store.builder()
                .id(3L)
                .name("스타벅스 강남점")
                .bizCategory("   ")
                .build();
        ExpenditureCreateRequest request = new ExpenditureCreateRequest();
        request.setCategoryId(category.getId());
        request.setStoreName(storeWithBlankBusinessCategory.getName());
        request.setBusinessCategory("  카페  ");
        request.setAmount(5_000L);
        request.setExpenditureDate(LocalDateTime.of(2026, 8, 18, 12, 0));
        when(categoryRepository.findAccessibleCategory(category.getId(), member))
                .thenReturn(Optional.of(category));
        when(storeRepository.findByName(storeWithBlankBusinessCategory.getName()))
                .thenReturn(Optional.of(storeWithBlankBusinessCategory));
        when(expenditureRepository.save(org.mockito.ArgumentMatchers.any(Expenditure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        expenditureService.createExpenditure(member, request);

        assertThat(storeWithBlankBusinessCategory.getBizCategory()).isEqualTo("카페");
    }

    @Test
    void 선택한_카카오_장소의_좌표를_가맹점에_저장한다() {
        ExpenditureCreateRequest request = new ExpenditureCreateRequest();
        request.setCategoryId(category.getId());
        request.setStoreName("스타벅스 홍대점");
        request.setPlaceId("123456789");
        request.setAddress("서울 마포구 홍익로 1");
        request.setLatitude(new BigDecimal("37.5561000"));
        request.setLongitude(new BigDecimal("126.9236000"));
        request.setAmount(5_000L);
        request.setExpenditureDate(LocalDateTime.of(2026, 8, 25, 12, 0));
        when(categoryRepository.findAccessibleCategory(category.getId(), member))
                .thenReturn(Optional.of(category));
        when(storeRepository.findByKakaoPlaceId("123456789")).thenReturn(Optional.empty());
        when(storeRepository.save(org.mockito.ArgumentMatchers.any(Store.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(expenditureRepository.save(org.mockito.ArgumentMatchers.any(Expenditure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        expenditureService.createExpenditure(member, request);

        ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
        verify(storeRepository).save(captor.capture());
        assertThat(captor.getValue().getKakaoPlaceId()).isEqualTo("123456789");
        assertThat(captor.getValue().getAddress()).isEqualTo("서울 마포구 홍익로 1");
        assertThat(captor.getValue().getLatitude()).isEqualByComparingTo("37.5561000");
        assertThat(captor.getValue().getLongitude()).isEqualByComparingTo("126.9236000");
        verify(storeRepository, never()).findByName("스타벅스 홍대점");
    }

    @Test
    void 동일한_카카오_장소는_기존_가맹점을_재사용한다() {
        Store existingStore = Store.builder()
                .id(3L)
                .name("스타벅스 홍대점")
                .kakaoPlaceId("123456789")
                .build();
        ExpenditureCreateRequest request = new ExpenditureCreateRequest();
        request.setCategoryId(category.getId());
        request.setStoreName(existingStore.getName());
        request.setPlaceId(existingStore.getKakaoPlaceId());
        request.setAddress("서울 마포구 홍익로 1");
        request.setLatitude(new BigDecimal("37.5561000"));
        request.setLongitude(new BigDecimal("126.9236000"));
        request.setAmount(5_000L);
        request.setExpenditureDate(LocalDateTime.of(2026, 8, 25, 12, 0));
        when(categoryRepository.findAccessibleCategory(category.getId(), member))
                .thenReturn(Optional.of(category));
        when(storeRepository.findByKakaoPlaceId(existingStore.getKakaoPlaceId()))
                .thenReturn(Optional.of(existingStore));
        when(expenditureRepository.save(org.mockito.ArgumentMatchers.any(Expenditure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        expenditureService.createExpenditure(member, request);

        verify(storeRepository, never()).save(org.mockito.ArgumentMatchers.any(Store.class));
        assertThat(existingStore.getAddress()).isEqualTo("서울 마포구 홍익로 1");
        assertThat(existingStore.getLatitude()).isEqualByComparingTo("37.5561000");
        assertThat(existingStore.getLongitude()).isEqualByComparingTo("126.9236000");
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

    @Test
    void 월별_지출_목록에_가맹점_위치_정보를_반환한다() {
        Store locatedStore = createLocatedStore();
        Expenditure expenditure = createExpenditure(locatedStore);
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 1, 0, 0);
        when(expenditureRepository.findByMonth(member, start, end))
                .thenReturn(List.of(expenditure));

        ExpenditureListResponse response = expenditureService.getExpenditureList(member, 2026, 8);

        ExpenditureElement element = response.getDailyExpenditures().getFirst().getList().getFirst();
        assertThat(element.getPlaceId()).isEqualTo("123456789");
        assertThat(element.getAddress()).isEqualTo("서울 마포구 홍익로 1");
        assertThat(element.getLatitude()).isEqualByComparingTo("37.5561000");
        assertThat(element.getLongitude()).isEqualByComparingTo("126.9236000");
    }

    @Test
    void 지출_상세에_가맹점_위치_정보를_반환한다() {
        Store locatedStore = createLocatedStore();
        Expenditure expenditure = createExpenditure(locatedStore);
        when(expenditureRepository.findByIdAndMember(expenditure.getId(), member))
                .thenReturn(Optional.of(expenditure));

        ExpenditureDetailResponse response = expenditureService.getExpenditureDetail(
                member,
                expenditure.getId()
        );

        assertThat(response.getPlaceId()).isEqualTo("123456789");
        assertThat(response.getAddress()).isEqualTo("서울 마포구 홍익로 1");
        assertThat(response.getLatitude()).isEqualByComparingTo("37.5561000");
        assertThat(response.getLongitude()).isEqualByComparingTo("126.9236000");
    }

    @Test
    void 지출의_카카오_장소를_수정하면_가맹점_연관관계와_응답이_변경된다() {
        Expenditure expenditure = createExpenditure();
        ExpenditureUpdateRequest request = org.mockito.Mockito.mock(ExpenditureUpdateRequest.class);
        Store selectedStore = createLocatedStore();
        when(request.getCategoryId()).thenReturn(category.getId());
        when(request.getPlaceId()).thenReturn(selectedStore.getKakaoPlaceId());
        when(request.getStoreName()).thenReturn(selectedStore.getName());
        when(request.getAddress()).thenReturn("서울 마포구 새 주소 2");
        when(request.getLatitude()).thenReturn(new BigDecimal("37.5570000"));
        when(request.getLongitude()).thenReturn(new BigDecimal("126.9240000"));
        when(expenditureRepository.findByIdAndMember(expenditure.getId(), member))
                .thenReturn(Optional.of(expenditure));
        when(storeRepository.findByKakaoPlaceId(selectedStore.getKakaoPlaceId()))
                .thenReturn(Optional.of(selectedStore));

        ExpenditureUpdateResponse response = expenditureService.updateExpenditure(
                member,
                expenditure.getId(),
                request
        );

        assertThat(expenditure.getStore()).isSameAs(selectedStore);
        assertThat(response.getPlaceId()).isEqualTo("123456789");
        assertThat(response.getAddress()).isEqualTo("서울 마포구 새 주소 2");
        assertThat(response.getLatitude()).isEqualByComparingTo("37.5570000");
        assertThat(response.getLongitude()).isEqualByComparingTo("126.9240000");
    }

    private Expenditure createExpenditure() {
        return createExpenditure(store);
    }

    private Expenditure createExpenditure(Store expenditureStore) {
        return Expenditure.builder()
                .id(10L)
                .member(member)
                .category(category)
                .store(expenditureStore)
                .amount(5_000L)
                .inputType(InputType.MANUAL)
                .expenditureDate(LocalDateTime.of(2026, 8, 18, 12, 0))
                .currency(Currency.KRW)
                .build();
    }

    private Store createLocatedStore() {
        return Store.builder()
                .id(4L)
                .name("스타벅스 홍대점")
                .kakaoPlaceId("123456789")
                .address("서울 마포구 홍익로 1")
                .latitude(new BigDecimal("37.5561000"))
                .longitude(new BigDecimal("126.9236000"))
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
