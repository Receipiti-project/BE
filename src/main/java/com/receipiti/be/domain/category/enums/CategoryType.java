package com.receipiti.be.domain.category.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CategoryType {
    FOOD("식비"),
    TRANSPORT("교통"),
    SHOPPING("쇼핑"),
    CULTURE("문화/여가"),
    HEALTH("건강/의료"),
    ETC("기타");

    private final String description;

}
