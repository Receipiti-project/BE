package com.receipiti.be.domain.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.receipiti.be.domain.category.dto.request.CategoryRequest;
import com.receipiti.be.domain.category.dto.response.CategoryResponse;
import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.category.enums.CategoryType;
import com.receipiti.be.domain.category.repository.CategoryRepository;
import com.receipiti.be.domain.expenditure.repository.ExpenditureRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ExpenditureRepository expenditureRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Member member;
    private Category customCategory;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).build();
        customCategory = Category.builder()
                .id(10L)
                .member(member)
                .categoryType(CategoryType.CUSTOM)
                .name("기존 이름")
                .build();
    }

    @Test
    void 본인의_커스텀_카테고리_규칙을_수정한다() {
        CategoryRequest request = new CategoryRequest("새 이름");
        when(categoryRepository.findAccessibleCategoryForUpdate(10L, member)).thenReturn(Optional.of(customCategory));

        CategoryResponse response = categoryService.updateCategoryRule(member, 10L, request);

        assertThat(response.getName()).isEqualTo("새 이름");
        assertThat(customCategory.getName()).isEqualTo("새 이름");
    }

    @Test
    void 사용_중인_카테고리_규칙은_삭제할_수_없다() {
        when(categoryRepository.findAccessibleCategoryForUpdate(10L, member)).thenReturn(Optional.of(customCategory));
        when(expenditureRepository.existsByCategory(customCategory)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategoryRule(member, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.CATEGORY_IN_USE);

        verify(categoryRepository, never()).delete(customCategory);
    }

    @Test
    void 본인의_사용하지_않는_카테고리_규칙을_삭제한다() {
        when(categoryRepository.findAccessibleCategoryForUpdate(10L, member)).thenReturn(Optional.of(customCategory));
        when(expenditureRepository.existsByCategory(customCategory)).thenReturn(false);

        categoryService.deleteCategoryRule(member, 10L);

        verify(categoryRepository).delete(customCategory);
    }

    @Test
    void 다른_사용자의_카테고리_규칙에는_접근할_수_없다() {
        when(categoryRepository.findAccessibleCategoryForUpdate(10L, member)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategoryRule(member, 10L, new CategoryRequest("새 이름")))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void 기본_카테고리_규칙은_수정할_수_없다() {
        Category defaultCategory = Category.builder()
                .id(1L)
                .categoryType(CategoryType.FOOD)
                .name("식비")
                .build();
        when(categoryRepository.findAccessibleCategoryForUpdate(1L, member))
                .thenReturn(Optional.of(defaultCategory));

        assertThatThrownBy(() -> categoryService.updateCategoryRule(
                member, 1L, new CategoryRequest("변경된 식비")))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.CATEGORY_MODIFICATION_FORBIDDEN);
    }

}
