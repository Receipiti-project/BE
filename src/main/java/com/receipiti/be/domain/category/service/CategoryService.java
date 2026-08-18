package com.receipiti.be.domain.category.service;

import com.receipiti.be.domain.category.dto.request.CategoryRequest;
import com.receipiti.be.domain.category.dto.response.CategoryResponse;
import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.category.enums.CategoryType;
import com.receipiti.be.domain.category.repository.CategoryRepository;
import com.receipiti.be.domain.expenditure.repository.ExpenditureRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ExpenditureRepository expenditureRepository;

    // 전체 카테고리 목록 조회 (공통 + 커스텀)
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryList(Member member) {
        List<Category> categories = categoryRepository.findByMemberIsNullOrMember(member);
        return categories.stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    // 커스텀 카테고리 생성
    public CategoryResponse createCustomCategory(Member member, CategoryRequest request) {
        if (member == null) {
            throw new IllegalArgumentException("커스텀 카테고리를 생성하려면 회원 정보가 필수적입니다.");
        }
        Category customCategory = Category.builder()
                .member(member)
                .name(request.getName())
                .categoryType(CategoryType.CUSTOM) //개인이 만드는 건 CUSTOM
                .build();

        Category saved = categoryRepository.save(customCategory);
        return CategoryResponse.from(saved);
    }

    public CategoryResponse updateCategoryRule(Member member, Long categoryId, CategoryRequest request) {
        Category category = getOwnedCustomCategory(member, categoryId);
        category.updateName(request.getName());
        return CategoryResponse.from(category);
    }

    public void deleteCategoryRule(Member member, Long categoryId) {
        Category category = getOwnedCustomCategory(member, categoryId);

        if (expenditureRepository.existsByCategory(category)) {
            throw new GeneralException(GeneralErrorCode.CATEGORY_IN_USE);
        }

        categoryRepository.delete(category);
    }

    private Category getOwnedCustomCategory(Member member, Long categoryId) {
        if (member == null) {
            throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
        }

        Category category = categoryRepository.findByIdAndMember(categoryId, member)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.CATEGORY_NOT_FOUND));

        if (category.getCategoryType() != CategoryType.CUSTOM) {
            throw new GeneralException(GeneralErrorCode.CATEGORY_MODIFICATION_FORBIDDEN);
        }

        return category;
    }
}
