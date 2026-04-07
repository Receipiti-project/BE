package com.receipiti.be.domain.expenditure.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InputType {
    OCR("영수증 업로드"),
    VOICE("음성 입력"),
    MANUAL("직접 입력"),
    SMS("문자 파싱");

    private final String description;
}
