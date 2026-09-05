package com.receipiti.be.domain.expenditure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.receipiti.be.domain.expenditure.dto.response.CardNotificationAnalysisResponse;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class CardNotificationAnalysisServiceTest {

    private GeminiCardNotificationClient geminiClient;
    private CardNotificationAnalysisService service;

    @BeforeEach
    void setUp() {
        geminiClient = mock(GeminiCardNotificationClient.class);
        service = new CardNotificationAnalysisService(geminiClient, new ObjectMapper());
    }

    @Test
    void 카드_결제_알림_이미지를_구조화된_소비_정보로_변환한다() {
        given(geminiClient.analyze(anyString(), any(byte[].class), anyString()))
                .willReturn(validResponse(0.96));
        MockMultipartFile file = imageFile();

        CardNotificationAnalysisResponse response = service.analyze(file);

        assertThat(response.paymentNotification()).isTrue();
        assertThat(response.cardCompany()).isEqualTo("신한카드");
        assertThat(response.storeName()).isEqualTo("스타벅스 홍대점");
        assertThat(response.amount()).isEqualTo(5_500L);
        assertThat(response.currency()).isEqualTo("KRW");
        assertThat(response.approvalStatus()).isEqualTo("APPROVED");
    }

    @Test
    void 카드_결제_정보가_부족하면_분석_결과를_반환하지_않는다() {
        given(geminiClient.analyze(anyString(), any(byte[].class), anyString()))
                .willReturn("""
                        {"paymentNotification":false,"cardCompany":"","storeName":"","amount":0,
                        "paymentDateTime":"","currency":"","approvalStatus":"UNKNOWN","confidence":0.1}
                        """);

        assertInsufficient(imageFile());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 1.1})
    void 신뢰도가_0과_1_사이를_벗어나면_분석_결과를_반환하지_않는다(double confidence) {
        given(geminiClient.analyze(anyString(), any(byte[].class), anyString()))
                .willReturn(validResponse(confidence));

        assertInsufficient(imageFile());
    }

    @Test
    void 지원하지_않는_파일은_Gemini에_전송하지_않는다() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notification.txt", MediaType.TEXT_PLAIN_VALUE, "text".getBytes()
        );

        assertThatThrownBy(() -> service.analyze(file))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.UNSUPPORTED_IMAGE_TYPE);
        verifyNoInteractions(geminiClient);
    }

    private void assertInsufficient(MockMultipartFile file) {
        assertThatThrownBy(() -> service.analyze(file))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.CARD_NOTIFICATION_INFORMATION_INSUFFICIENT);
    }

    private MockMultipartFile imageFile() {
        return new MockMultipartFile(
                "file", "card-notification.png", MediaType.IMAGE_PNG_VALUE, "image-data".getBytes()
        );
    }

    private String validResponse(double confidence) {
        return """
                {"paymentNotification":true,"cardCompany":"신한카드","storeName":"스타벅스 홍대점",
                "amount":5500,"paymentDateTime":"2026-09-04T16:30:00","currency":"KRW",
                "approvalStatus":"APPROVED","confidence":%s}
                """.formatted(confidence);
    }
}
