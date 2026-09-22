package com.receipiti.be.domain.expenditure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.receipiti.be.domain.expenditure.dto.response.OcrResponse;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ReceiptAnalysisServiceTest {

    @Mock
    private NaverOcrHandler naverOcrHandler;
    @Mock
    private GeminiReceiptClient geminiReceiptClient;

    private ReceiptAnalysisService service;
    private MockMultipartFile image;

    @BeforeEach
    void setUp() {
        service = new ReceiptAnalysisService(naverOcrHandler, geminiReceiptClient, new ObjectMapper());
        image = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    @Test
    void Gemini가_브랜드명과_지점명을_포함한_전체_매장명을_추출한다() {
        when(geminiReceiptClient.analyze(anyString(), any(byte[].class), anyString())).thenReturn("""
                {"storeName":"스타벅스 숙명여대점","amount":5500,
                "paymentDate":"2026-09-22T14:30:00","confidence":0.96}
                """);

        OcrResponse result = service.analyze(image);

        assertThat(result.getStoreName()).isEqualTo("스타벅스 숙명여대점");
        assertThat(result.getAmount()).isEqualTo(5_500L);
        assertThat(result.getCorrectedByLlm()).isTrue();
        verify(naverOcrHandler, never()).executeOcr(image);
    }

    @Test
    void Gemini_분석_결과가_불충분하면_네이버_OCR로_fallback한다() {
        when(geminiReceiptClient.analyze(anyString(), any(byte[].class), anyString())).thenReturn("""
                {"storeName":"","amount":0,"paymentDate":"","confidence":0.2}
                """);
        when(naverOcrHandler.executeOcr(image)).thenReturn(OcrResponse.builder()
                .storeName("카페 낭")
                .amount(25_800L)
                .paymentDate(LocalDateTime.of(2026, 7, 16, 13, 4, 48))
                .confidence(0.95)
                .build());

        OcrResponse result = service.analyze(image);

        assertThat(result.getStoreName()).isEqualTo("카페 낭");
        assertThat(result.getAmount()).isEqualTo(25_800L);
        assertThat(result.getCorrectedByLlm()).isFalse();
        verify(geminiReceiptClient).analyze(anyString(), any(byte[].class), anyString());
        verify(naverOcrHandler).executeOcr(image);
    }

    @Test
    void Gemini_호출이_실패해도_네이버_OCR로_fallback한다() {
        when(geminiReceiptClient.analyze(anyString(), any(byte[].class), anyString()))
                .thenThrow(new RuntimeException("Gemini unavailable"));
        when(naverOcrHandler.executeOcr(image)).thenReturn(OcrResponse.builder()
                .storeName("레드버튼(신촌점)")
                .amount(30_100L)
                .paymentDate(LocalDateTime.of(2026, 9, 3, 21, 31, 44))
                .confidence(0.91)
                .build());

        OcrResponse result = service.analyze(image);

        assertThat(result.getStoreName()).isEqualTo("레드버튼(신촌점)");
        assertThat(result.getCorrectedByLlm()).isFalse();
    }

    @Test
    void Gemini와_네이버_OCR_결과가_모두_불충분하면_정보_부족_오류를_반환한다() {
        when(geminiReceiptClient.analyze(anyString(), any(byte[].class), anyString())).thenReturn("""
                {"storeName":"스타벅스","amount":0,"paymentDate":"","confidence":0.3}
                """);
        when(naverOcrHandler.executeOcr(image)).thenReturn(OcrResponse.builder()
                .storeName("알 수 없는 상호명")
                .amount(0L)
                .confidence(0.4)
                .build());

        assertThatThrownBy(() -> service.analyze(image))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.RECEIPT_INFORMATION_INSUFFICIENT);
    }
}
