package com.receipiti.be.global.apiPayload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.receipiti.be.global.apiPayload.code.BaseSuccessCode;
import com.receipiti.be.global.apiPayload.code.GeneralSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {

    private final Boolean isSuccess;
    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    // 일반적인 성공 응답
    public static <T> ApiResponse<T> onSuccess(T result) {
        return ApiResponse.of(GeneralSuccessCode._OK, result);
    }

    // 상황별 성공 응답
    public static <T> ApiResponse<T> of(BaseSuccessCode code, T result) {
        return new ApiResponse<>(true, code.getCode(), code.getMessage(), result);
    }

    // 실패 응답
    public static <T> ApiResponse<T> onFailure(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }
}