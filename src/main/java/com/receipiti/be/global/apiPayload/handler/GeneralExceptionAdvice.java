package com.receipiti.be.global.apiPayload.handler;

import com.receipiti.be.global.apiPayload.ApiResponse;
import com.receipiti.be.global.apiPayload.code.BaseErrorCode;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GeneralExceptionAdvice extends ResponseEntityExceptionHandler {
    
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<Object> onThrowException(GeneralException exception, HttpServletRequest request) {
        BaseErrorCode code = exception.getCode();
        log.warn("비즈니스 로직 에러 발생: {} - {}", code.getCode(), code.getMessage());

        return handleExceptionInternal(code, null);
    }

    // 그 외 모든 예기치 못한 에러 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> onException(Exception exception, HttpServletRequest request) {
        log.error("시스템 에러 발생", exception);
        return handleExceptionInternal(GeneralErrorCode.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    private ResponseEntity<Object> handleExceptionInternal(BaseErrorCode code, Object errorData) {
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.onFailure(code.getCode(), code.getMessage(), errorData));
    }
}
