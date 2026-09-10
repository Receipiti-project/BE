package com.receipiti.be.domain.expenditure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.receipiti.be.domain.expenditure.dto.response.OcrResponse;
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
    void 신뢰할_수_있는_OCR_결과는_LLM을_호출하지_않는다() {
        when(naverOcrHandler.executeOcr(image)).thenReturn(OcrResponse.builder()
                .storeName("카페 낭")
                .amount(25_800L)
                .paymentDate(LocalDateTime.of(2026, 7, 16, 13, 4, 48))
                .confidence(0.95)
                .build());

        OcrResponse result = service.analyze(image);

        assertThat(result.getAmount()).isEqualTo(25_800L);
        assertThat(result.getCorrectedByLlm()).isFalse();
        verify(geminiReceiptClient, never()).analyze(anyString(), any(byte[].class), anyString());
    }

    @Test
    void 신뢰도가_낮은_OCR_결과는_Gemini로_보정한다() {
        when(naverOcrHandler.executeOcr(image)).thenReturn(OcrResponse.builder()
                .storeName("카페")
                .amount(130_448L)
                .paymentDate(LocalDateTime.of(2026, 7, 16, 13, 4, 48))
                .confidence(0.5)
                .build());
        when(geminiReceiptClient.analyze(anyString(), any(byte[].class), anyString())).thenReturn("""
                {"storeName":"카페 낭","amount":25800,"paymentDate":"2026-07-16T13:04:48","confidence":0.96}
                """);

        OcrResponse result = service.analyze(image);

        assertThat(result.getStoreName()).isEqualTo("카페 낭");
        assertThat(result.getAmount()).isEqualTo(25_800L);
        assertThat(result.getCorrectedByLlm()).isTrue();
        verify(geminiReceiptClient).analyze(anyString(), any(byte[].class), anyString());
    }

    @Test
    void 신뢰도가_높아도_상호명이_기호뿐이면_Gemini로_보정한다() {
        when(naverOcrHandler.executeOcr(image)).thenReturn(OcrResponse.builder()
                .storeName("[")
                .amount(30_100L)
                .paymentDate(LocalDateTime.of(2026, 9, 3, 21, 31, 44))
                .confidence(0.91)
                .build());
        when(geminiReceiptClient.analyze(anyString(), any(byte[].class), anyString())).thenReturn("""
                {"storeName":"레드버튼(신촌점)","amount":30100,"paymentDate":"2026-09-03T21:31:44","confidence":0.97}
                """);

        OcrResponse result = service.analyze(image);

        assertThat(result.getStoreName()).isEqualTo("레드버튼(신촌점)");
        assertThat(result.getCorrectedByLlm()).isTrue();
        verify(geminiReceiptClient).analyze(anyString(), any(byte[].class), anyString());
    }
}
