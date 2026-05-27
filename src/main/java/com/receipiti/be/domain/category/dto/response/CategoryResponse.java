package com.receipiti.be.domain.category.dto.response;

import com.receipiti.be.domain.category.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CategoryResponse {
    private Long categoryId;
    private String name;
    private String categoryType;
    private boolean isCustom;

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getId())
                .name(category.getName())
                .categoryType(category.getCategoryType().name())
                .isCustom(category.getMember() != null) // member가 있으면 내가 만든 커스텀
                .build();
    }
}