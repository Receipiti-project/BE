package com.receipiti.be.domain.category.controller;

import com.receipiti.be.domain.category.docs.CategoryApiDocs;
import com.receipiti.be.domain.category.dto.request.CategoryRequest;
import com.receipiti.be.domain.category.dto.response.CategoryResponse;
import com.receipiti.be.domain.category.service.CategoryService;
import com.receipiti.be.domain.member.entity.Member;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController implements CategoryApiDocs {

    private final CategoryService categoryService;

    @Override
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategoryList(
            @AuthenticationPrincipal Member member)
    {
        List<CategoryResponse> response = categoryService.getCategoryList(member);
        return  ResponseEntity.ok(response);
    }

    @Override
    @PostMapping
    public ResponseEntity<CategoryResponse> createCustomCategory(
            @AuthenticationPrincipal Member member,
            @jakarta.validation.Valid @RequestBody CategoryRequest request
    ){
        CategoryResponse response = categoryService.createCustomCategory(member, request);
        return ResponseEntity.ok(response);
    }
}
