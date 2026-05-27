package com.receipiti.be.domain.category.service;

import com.receipiti.be.domain.category.dto.request.CategoryRequest;
import com.receipiti.be.domain.category.dto.response.CategoryResponse;
import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.category.enums.CategoryType;
import com.receipiti.be.domain.category.repository.CategoryRepository;
import com.receipiti.be.domain.member.entity.Member;
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
        Category customCategory = Category.builder()
                .member(member)
                .name(request.getName())
                .categoryType(CategoryType.CUSTOM) //개인이 만드는 건 CUSTOM
                .build();

        Category saved = categoryRepository.save(customCategory);
        return CategoryResponse.from(saved);
    }
}