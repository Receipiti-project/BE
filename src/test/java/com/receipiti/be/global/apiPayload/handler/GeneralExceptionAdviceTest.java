package com.receipiti.be.global.apiPayload.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.receipiti.be.global.apiPayload.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GeneralExceptionAdviceTest {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private final GeneralExceptionAdvice advice = new GeneralExceptionAdvice();

    @Test
    void returnsDedicatedErrorResponseWhenUploadSizeIsExceeded() {
        ResponseEntity<Object> response = advice.handleMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(MAX_FILE_SIZE),
                HttpHeaders.EMPTY,
                HttpStatus.PAYLOAD_TOO_LARGE,
                new ServletWebRequest(new MockHttpServletRequest())
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isInstanceOfSatisfying(ApiResponse.class, body -> {
            assertThat(body.getIsSuccess()).isFalse();
            assertThat(body.getCode()).isEqualTo("FILE_413");
            assertThat(body.getMessage()).isEqualTo("파일 크기는 최대 20MB까지 업로드할 수 있습니다.");
            assertThat(body.getResult()).isNull();
        });
    }
}
