package com.receipiti.be.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GeneralErrorCode implements BaseErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 에러입니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_403", "요청이 거부되었습니다."),
    KAKAO_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_KAKAO_401", "카카오 로그인에 실패했습니다."),
    INVALID_LOGIN_CODE(HttpStatus.UNAUTHORIZED, "AUTH_LOGIN_CODE_401", "유효하지 않거나 만료된 로그인 코드입니다."),
    KAKAO_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "AUTH_KAKAO_502", "카카오 인증 서버 응답에 실패했습니다."),
    KAKAO_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_KAKAO_503", "카카오 인증 서버에 연결할 수 없습니다."),
    PLACE_SEARCH_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "PLACE_SEARCH_502", "장소 검색 서버 응답에 실패했습니다."),
    PLACE_SEARCH_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PLACE_SEARCH_503", "장소 검색 서버에 연결할 수 없습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_413", "파일 크기는 최대 20MB까지 업로드할 수 있습니다."),
    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "IMAGE_400", "분석할 이미지 파일이 필요합니다."),
    UNSUPPORTED_IMAGE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "IMAGE_415", "지원하지 않는 이미지 형식입니다."),
    CARD_NOTIFICATION_INFORMATION_INSUFFICIENT(HttpStatus.UNPROCESSABLE_ENTITY, "CARD_NOTIFICATION_422", "카드 결제 정보를 충분히 확인할 수 없습니다."),
    CARD_NOTIFICATION_ANALYSIS_FAILED(HttpStatus.BAD_GATEWAY, "CARD_NOTIFICATION_502", "카드 결제 알림 이미지 분석에 실패했습니다."),
    AI_REPORT_RESPONSE_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "AI_REPORT_422", "AI 소비 리포트 응답 형식이 올바르지 않습니다."),
    AI_REPORT_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "AI_REPORT_502", "AI 소비 리포트 생성에 실패했습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "요청한 리소스를 찾을 수 없습니다."),
    EXPENDITURE_NOT_FOUND(HttpStatus.NOT_FOUND, "EXPENDITURE_404", "존재하지 않거나 접근 권한이 없는 지출 내역입니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_404", "존재하지 않거나 접근 권한이 없는 카테고리입니다."),
    CATEGORY_MODIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "CATEGORY_403", "기본 카테고리는 수정하거나 삭제할 수 없습니다."),
    CATEGORY_IN_USE(HttpStatus.CONFLICT, "CATEGORY_409", "사용 중인 카테고리는 삭제할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
