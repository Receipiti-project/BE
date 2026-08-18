package com.receipiti.be.domain.category.docs;

import com.receipiti.be.domain.category.dto.request.CategoryRequest;
import com.receipiti.be.domain.category.dto.response.CategoryResponse;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@Tag(name = "Category", description = "카테고리 관련 API")
public interface CategoryApiDocs {

    @Operation(summary = "전체 카테고리 목록 조회", description = "기본 공통 카테고리와 유저의 커스텀 카테고리를 합쳐서 반환합니다.")
    ResponseEntity<List<CategoryResponse>> getCategoryList(
            @Parameter(hidden = true)Member member);

    @Operation(summary = "커스텀 카테고리 생성", description = "유저만의 커스텀 카테고리를 추가합니다.")
    ResponseEntity<CategoryResponse> createCustomCategory(
            @Parameter(hidden = true)Member member,
            @RequestBody CategoryRequest request);

    @Operation(summary = "카테고리 규칙 수정", description = "로그인한 사용자가 생성한 커스텀 카테고리의 이름을 수정합니다.")
    ResponseEntity<CategoryResponse> updateCategoryRule(
            @Parameter(hidden = true) Member member,
            @Parameter(description = "수정할 카테고리 ID", example = "1") Long id,
            @RequestBody CategoryRequest request);

    @Operation(summary = "카테고리 규칙 삭제", description = "로그인한 사용자가 생성한 커스텀 카테고리를 삭제합니다. 사용 중인 카테고리는 삭제할 수 없습니다.")
    ResponseEntity<ApiResponse<String>> deleteCategoryRule(
            @Parameter(hidden = true) Member member,
            @Parameter(description = "삭제할 카테고리 ID", example = "1") Long id);
}
