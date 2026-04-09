package com.receipiti.be.global.apiPayload.handler;

import com.receipiti.be.global.apiPayload.ApiResponse;
import com.receipiti.be.global.apiPayload.code.BaseErrorCode;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GeneralExceptionAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<Object> onThrowException(GeneralException exception, HttpServletRequest request) {
        BaseErrorCode code = exception.getCode();
        log.warn("비즈니스 로직 에러 발생: {} - {}", code.getCode(), code.getMessage());
        return getExceptionResponseEntity(code, null);
    }

    // 그 외 모든 예기치 못한 에러 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> onException(Exception exception, HttpServletRequest request) {
        log.error("시스템 에러 발생", exception);
        return getExceptionResponseEntity(GeneralErrorCode.INTERNAL_SERVER_ERROR, null);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        log.warn("스프링 표준 예외 발생: {}", ex.getMessage());


        return super.handleExceptionInternal(
                ex,
                ApiResponse.onFailure(
                        "COMMON_" + statusCode.value(),
                        HttpStatus.valueOf(statusCode.value()).getReasonPhrase(),
                        ex.getMessage()
                ),
                headers,
                statusCode,
                request
        );
    }

    private ResponseEntity<Object> getExceptionResponseEntity(BaseErrorCode code, Object errorData) {
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.onFailure(code.getCode(), code.getMessage(), errorData));
    }
}