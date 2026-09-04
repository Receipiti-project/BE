package com.receipiti.be.domain.expenditure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.receipiti.be.domain.expenditure.dto.response.CardNotificationAnalysisResponse;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CardNotificationAnalysisServiceTest {

    private MockRestServiceServer server;
    private CardNotificationAnalysisService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new CardNotificationAnalysisService(builder.build(), new ObjectMapper());
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
    }

    @Test
    void 카드_결제_알림_이미지를_구조화된_소비_정보로_변환한다() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/"
                        + "gemini-2.5-flash:generateContent?key=test-key"))
                .andRespond(withSuccess("""
                        {
                          "candidates": [{
                            "content": {
                              "parts": [{
                                "text": "{\\\"paymentNotification\\\":true,\\\"cardCompany\\\":\\\"신한카드\\\",\\\"storeName\\\":\\\"스타벅스 홍대점\\\",\\\"amount\\\":5500,\\\"paymentDateTime\\\":\\\"2026-09-04T16:30:00\\\",\\\"currency\\\":\\\"KRW\\\",\\\"approvalStatus\\\":\\\"APPROVED\\\",\\\"confidence\\\":0.96}"
                              }]
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "card-notification.png",
                MediaType.IMAGE_PNG_VALUE,
                "image-data".getBytes()
        );

        CardNotificationAnalysisResponse response = service.analyze(file);

        assertThat(response.paymentNotification()).isTrue();
        assertThat(response.cardCompany()).isEqualTo("신한카드");
        assertThat(response.storeName()).isEqualTo("스타벅스 홍대점");
        assertThat(response.amount()).isEqualTo(5_500L);
        assertThat(response.currency()).isEqualTo("KRW");
        assertThat(response.approvalStatus()).isEqualTo("APPROVED");
        server.verify();
    }

    @Test
    void 카드_결제_정보가_부족하면_분석_결과를_반환하지_않는다() {
        server.expect(requestTo(org.hamcrest.Matchers.any(String.class)))
                .andRespond(withSuccess("""
                        {
                          "candidates": [{
                            "content": {
                              "parts": [{
                                "text": "{\\\"paymentNotification\\\":false,\\\"cardCompany\\\":\\\"\\\",\\\"storeName\\\":\\\"\\\",\\\"amount\\\":0,\\\"paymentDateTime\\\":\\\"\\\",\\\"currency\\\":\\\"\\\",\\\"approvalStatus\\\":\\\"UNKNOWN\\\",\\\"confidence\\\":0.1}"
                              }]
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));
        MockMultipartFile file = new MockMultipartFile(
                "file", "screen.png", MediaType.IMAGE_PNG_VALUE, "image-data".getBytes()
        );

        assertThatThrownBy(() -> service.analyze(file))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.CARD_NOTIFICATION_INFORMATION_INSUFFICIENT);
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
        server.verify();
    }
}
