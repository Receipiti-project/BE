package com.receipiti.be.domain.categoryhistory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.categoryhistory.entity.CategorySelectionHistory;
import com.receipiti.be.domain.categoryhistory.enums.CategorySelectionSource;
import com.receipiti.be.domain.categoryhistory.repository.CategorySelectionHistoryRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategorySelectionHistoryServiceTest {

    @Mock
    private CategorySelectionHistoryRepository categorySelectionHistoryRepository;

    @Mock
    private MerchantNameNormalizer merchantNameNormalizer;

    @InjectMocks
    private CategorySelectionHistoryService categorySelectionHistoryService;

    @Test
    void 카테고리_선택_당시의_가맹점_정보를_이력으로_저장한다() {
        Member member = Member.builder().id(1L).build();
        Category category = Category.builder().id(2L).build();
        Store store = Store.builder()
                .id(3L)
                .name("스타벅스 강남점")
                .bizCategory("카페")
                .build();
        org.mockito.Mockito.when(merchantNameNormalizer.normalizeStoreName(store.getName()))
                .thenReturn("스타벅스강남점");
        org.mockito.Mockito.when(merchantNameNormalizer.normalizeBrandName(store.getName()))
                .thenReturn("스타벅스");

        categorySelectionHistoryService.recordSelection(
                member,
                category,
                store,
                CategorySelectionSource.UPDATE
        );

        ArgumentCaptor<CategorySelectionHistory> captor =
                ArgumentCaptor.forClass(CategorySelectionHistory.class);
        verify(categorySelectionHistoryRepository).save(captor.capture());
        CategorySelectionHistory history = captor.getValue();
        assertThat(history.getMember()).isEqualTo(member);
        assertThat(history.getCategory()).isEqualTo(category);
        assertThat(history.getStore()).isEqualTo(store);
        assertThat(history.getStoreName()).isEqualTo("스타벅스 강남점");
        assertThat(history.getNormalizedStoreName()).isEqualTo("스타벅스강남점");
        assertThat(history.getNormalizedBrandName()).isEqualTo("스타벅스");
        assertThat(history.getBusinessCategory()).isEqualTo("카페");
        assertThat(history.getSelectionSource()).isEqualTo(CategorySelectionSource.UPDATE);
    }
}
