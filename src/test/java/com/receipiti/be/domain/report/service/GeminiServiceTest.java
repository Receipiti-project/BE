package com.receipiti.be.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.receipiti.be.domain.report.dto.response.ReportResponse;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeminiServiceTest {

    private GeminiReportClient geminiClient;
    private GeminiService service;

    @BeforeEach
    void setUp() {
        geminiClient = mock(GeminiReportClient.class);
        service = new GeminiService(geminiClient, new ObjectMapper());
    }

    @Test
    void 소비_데이터를_구조화된_AI_리포트로_변환한다() {
        given(geminiClient.generate(anyString())).willReturn(validResponse(38.5));

        ReportResponse response = service.generateExpenditureReport(
                "2026-09",
                "2026-09-04 18:30 식비 12000원"
        );

        assertThat(response.targetMonth()).isEqualTo("2026-09");
        assertThat(response.totalAmount()).isEqualTo(61_000L);
        assertThat(response.transactionCount()).isEqualTo(4);
        assertThat(response.anomalyDetected()).isFalse();
        assertThat(response.frequentSpendingTime().timeRange()).isEqualTo("18:00~21:00");
        assertThat(response.frequentSpendingDay().dayOfWeek()).isEqualTo("금요일");
        assertThat(response.topCategory().categoryName()).isEqualTo("식비");
        assertThat(response.spendingPatternInsights()).hasSize(2);
        assertThat(response.summary()).isNotBlank();
    }

    @Test
    void 카테고리_비율이_유효범위를_벗어나면_리포트를_반환하지_않는다() {
        given(geminiClient.generate(anyString())).willReturn(validResponse(101.0));

        assertThatThrownBy(() -> service.generateExpenditureReport("2026-09", "식비 12000원"))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.AI_REPORT_RESPONSE_INVALID);
    }

    @Test
    void 카테고리_비율이_카테고리_금액과_일치하지_않으면_리포트를_반환하지_않는다() {
        given(geminiClient.generate(anyString())).willReturn(validResponse(50.0));

        assertInvalidResponse();
    }

    @Test
    void Gemini_응답이_잘못된_JSON이면_응답_검증_오류를_반환한다() {
        given(geminiClient.generate(anyString())).willReturn("{잘못된 JSON}");

        assertInvalidResponse();
    }

    @Test
    void 요일이_null이면_응답_검증_오류를_반환한다() {
        given(geminiClient.generate(anyString())).willReturn(
                validResponse(38.5).replace("\"dayOfWeek\":\"금요일\"", "\"dayOfWeek\":null")
        );

        assertInvalidResponse();
    }

    @Test
    void 하위_분석_금액이_총액보다_크면_응답_검증_오류를_반환한다() {
        given(geminiClient.generate(anyString())).willReturn(
                validResponse(38.5).replace("\"amount\":47000", "\"amount\":62000")
        );

        assertInvalidResponse();
    }

    @Test
    void 시간대가_정확히_3시간_간격이_아니면_응답_검증_오류를_반환한다() {
        given(geminiClient.generate(anyString())).willReturn(
                validResponse(38.5).replace("18:00~21:00", "18:00~20:00")
        );

        assertInvalidResponse();
    }

    @Test
    void 총액이_0이면_카테고리도_정보없음과_0으로_반환해야_한다() {
        given(geminiClient.generate(anyString())).willReturn(zeroAmountResponse());

        ReportResponse response = service.generateExpenditureReport("2026-09", "소비 내역 없음");

        assertThat(response.totalAmount()).isZero();
        assertThat(response.topCategory().categoryName()).isEqualTo("정보 없음");
        assertThat(response.topCategory().percentage()).isZero();
    }

    @Test
    void 분석월_형식이_잘못되면_Gemini를_호출하지_않는다() {
        assertThatThrownBy(() -> service.generateExpenditureReport("2026-13", "식비 12000원"))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.BAD_REQUEST);

        verifyNoInteractions(geminiClient);
    }

    @Test
    void 소비_데이터가_비어있으면_Gemini를_호출하지_않는다() {
        assertThatThrownBy(() -> service.generateExpenditureReport("2026-09", " "))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.BAD_REQUEST);

        verifyNoInteractions(geminiClient);
    }

    private void assertInvalidResponse() {
        assertThatThrownBy(() -> service.generateExpenditureReport("2026-09", "식비 12000원"))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.AI_REPORT_RESPONSE_INVALID);
    }

    private String validResponse(double percentage) {
        return """
                {
                  "targetMonth":"2026-09",
                  "totalAmount":61000,
                  "transactionCount":4,
                  "anomalyDetected":false,
                  "anomalyReason":"비교 가능한 이전 소비 데이터가 없어 이상 소비를 탐지하지 않았습니다.",
                  "frequentSpendingTime":{
                    "timeRange":"18:00~21:00","transactionCount":2,"amount":47000,
                    "description":"저녁 시간대에 소비가 집중됩니다."
                  },
                  "frequentSpendingDay":{
                    "dayOfWeek":"금요일","transactionCount":2,"amount":47000,
                    "description":"금요일 지출 비중이 가장 높습니다."
                  },
                  "topCategory":{
                    "categoryName":"식비","amount":23500,"percentage":%s,
                    "description":"식비가 가장 큰 지출 카테고리입니다."
                  },
                  "spendingPatternInsights":["저녁 소비가 집중됩니다.","금요일 지출이 많습니다."],
                  "summary":"9월 총지출은 61,000원이며 총 4건입니다. 저녁과 금요일에 소비가 집중되었습니다."
                }
                """.formatted(percentage);
    }

    private String zeroAmountResponse() {
        return """
                {
                  "targetMonth":"2026-09",
                  "totalAmount":0,
                  "transactionCount":0,
                  "anomalyDetected":false,
                  "anomalyReason":"분석할 소비 내역이 없습니다.",
                  "frequentSpendingTime":{
                    "timeRange":"정보 없음","transactionCount":0,"amount":0,
                    "description":"분석할 시간대 정보가 없습니다."
                  },
                  "frequentSpendingDay":{
                    "dayOfWeek":"정보 없음","transactionCount":0,"amount":0,
                    "description":"분석할 요일 정보가 없습니다."
                  },
                  "topCategory":{
                    "categoryName":"정보 없음","amount":0,"percentage":0.0,
                    "description":"분석할 카테고리 정보가 없습니다."
                  },
                  "spendingPatternInsights":["분석할 소비 내역이 없습니다."],
                  "summary":"9월에는 분석할 소비 내역이 없습니다."
                }
                """;
    }
}
