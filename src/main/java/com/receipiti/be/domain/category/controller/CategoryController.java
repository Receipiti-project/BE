package com.receipiti.be.domain.category.controller;

import com.receipiti.be.domain.category.docs.CategoryApiDocs;
import com.receipiti.be.domain.category.dto.request.CategoryRequest;
import com.receipiti.be.domain.category.dto.response.CategoryResponse;
import com.receipiti.be.domain.category.service.CategoryService;
import com.receipiti.be.domain.categoryhistory.dto.CategoryRecommendationResponse;
import com.receipiti.be.domain.categoryhistory.service.CategoryRecommendationQueryService;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.global.apiPayload.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController implements CategoryApiDocs {

    private final CategoryService categoryService;
    private final CategoryRecommendationQueryService categoryRecommendationQueryService;

    @Override
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategoryList(
            @AuthenticationPrincipal Member member)
    {
        List<CategoryResponse> response = categoryService.getCategoryList(member);
        return  ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/recommendation")
    public ResponseEntity<CategoryRecommendationResponse> getCategoryRecommendation(
            @AuthenticationPrincipal Member member,
            @RequestParam String storeName,
            @RequestParam(required = false) String businessCategory
    ) {
        return ResponseEntity.ok(categoryRecommendationQueryService.getRecommendation(
                member,
                storeName,
                businessCategory
        ));
    }

    @Override
    @PostMapping
    public ResponseEntity<CategoryResponse> createCustomCategory(
            @AuthenticationPrincipal Member member,
            @RequestBody CategoryRequest request
    ){
        CategoryResponse response = categoryService.createCustomCategory(member, request);
        return ResponseEntity.ok(response);
    }

    @Override
    @PatchMapping("/rules/{id}")
    public ResponseEntity<CategoryResponse> updateCategoryRule(
            @AuthenticationPrincipal Member member,
            @PathVariable Long id,
            @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategoryRule(member, id, request));
    }

    @Override
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCategoryRule(
            @AuthenticationPrincipal Member member,
            @PathVariable Long id) {
        categoryService.deleteCategoryRule(member, id);
        return ResponseEntity.ok(ApiResponse.onSuccess("카테고리 규칙 삭제가 완료되었습니다."));
    }
}
