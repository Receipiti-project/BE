package com.receipiti.be.global.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.category.enums.CategoryType;
import com.receipiti.be.domain.category.repository.CategoryRepository;
import com.receipiti.be.domain.expenditure.repository.ExpenditureRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ExpenditureRepository expenditureRepository;

    private DataInitializer dataInitializer;
    private Category defaultEtc;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer(categoryRepository, expenditureRepository);
        defaultEtc = Category.builder()
                .id(6L)
                .categoryType(CategoryType.ETC)
                .name("기타")
                .build();
        when(categoryRepository.findFirstByMemberIsNullAndCategoryType(CategoryType.ETC))
                .thenReturn(Optional.of(defaultEtc));
    }

    @Test
    void 누락된_기본_카테고리는_다른_기본_카테고리가_있어도_생성한다() {
        when(categoryRepository.existsByMemberIsNullAndCategoryType(any(CategoryType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0) != CategoryType.FOOD);

        dataInitializer.run(null);

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getCategoryType()).isEqualTo(CategoryType.FOOD);
        assertThat(categoryCaptor.getValue().getMember()).isNull();
    }

    @Test
    void 기존_전역_CUSTOM을_기타로_이관한_뒤_삭제한다() {
        when(categoryRepository.existsByMemberIsNullAndCategoryType(any(CategoryType.class)))
                .thenReturn(true);
        Category globalCustom = Category.builder()
                .id(7L)
                .categoryType(CategoryType.CUSTOM)
                .name("커스텀")
                .build();
        when(categoryRepository.findAllByMemberIsNullAndCategoryType(CategoryType.CUSTOM))
                .thenReturn(List.of(globalCustom));

        dataInitializer.run(null);

        verify(expenditureRepository).replaceCategory(globalCustom, defaultEtc);
        verify(categoryRepository).delete(globalCustom);
    }
}
