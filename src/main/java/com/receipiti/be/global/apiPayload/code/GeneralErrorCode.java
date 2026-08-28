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
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "요청한 리소스를 찾을 수 없습니다."),
    EXPENDITURE_NOT_FOUND(HttpStatus.NOT_FOUND, "EXPENDITURE_404", "존재하지 않거나 접근 권한이 없는 지출 내역입니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_404", "존재하지 않거나 접근 권한이 없는 카테고리입니다."),
    CATEGORY_MODIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "CATEGORY_403", "기본 카테고리는 수정하거나 삭제할 수 없습니다."),
    CATEGORY_IN_USE(HttpStatus.CONFLICT, "CATEGORY_409", "사용 중인 카테고리는 삭제할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
