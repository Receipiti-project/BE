package com.receipiti.be.domain.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {
    @NotBlank(message = "카테고리 이름은 필수 입력 값입니다.")
    @Size(max = 50, message = "카테고리 이름은 50자 이하로 입력해주세요.")
    private String name;
}
