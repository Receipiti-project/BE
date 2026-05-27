package com.receipiti.be.domain.category.docs;

import com.receipiti.be.domain.category.dto.request.CategoryRequest;
import com.receipiti.be.domain.category.dto.response.CategoryResponse;
import com.receipiti.be.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@Tag(name = "Category", description = "카테고리 관련 API")
public interface CategoryApiDocs {

    @Operation(summary = "전체 카테고리 목록 조회", description = "기본 공통 카테고리와 유저의 커스텀 카테고리를 합쳐서 반환합니다.")
    ResponseEntity<List<CategoryResponse>> getCategoryList(Member member);

    @Operation(summary = "커스텀 카테고리 생성", description = "유저만의 커스텀 카테고리를 추가합니다.")
    ResponseEntity<CategoryResponse> createCustomCategory(Member member, @RequestBody CategoryRequest request);
}