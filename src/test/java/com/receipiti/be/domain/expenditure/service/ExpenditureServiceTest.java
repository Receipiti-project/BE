package com.receipiti.be.domain.expenditure.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.category.repository.CategoryRepository;
import com.receipiti.be.domain.categoryhistory.enums.CategorySelectionSource;
import com.receipiti.be.domain.categoryhistory.service.CategorySelectionHistoryService;
import com.receipiti.be.domain.expenditure.dto.request.ExpenditureCreateRequest;
import com.receipiti.be.domain.expenditure.dto.request.ExpenditureUpdateRequest;
import com.receipiti.be.domain.expenditure.entity.Expenditure;
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

        expenditureService.createExpenditure(member, request);

        verify(categorySelectionHistoryService).recordSelection(
                member,
                category,
                store,
                CategorySelectionSource.CREATE
        );
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
}
