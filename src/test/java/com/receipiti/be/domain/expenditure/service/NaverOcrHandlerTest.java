package com.receipiti.be.domain.expenditure.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.receipiti.be.domain.expenditure.dto.response.OcrResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NaverOcrHandlerTest {

    private final NaverOcrHandler handler = new NaverOcrHandler();

    @Test
    void 결제시각을_금액으로_오인하지_않고_영수증_정보를_추출한다() {
        String response = """
                {
                  "images": [{
                    "fields": [
                      {"inferText":"[매장명]", "inferConfidence":0.99},
                      {"inferText":"카페 낭 (cafe nang)", "inferConfidence":0.98},
                      {"inferText":"사업자번호", "inferConfidence":0.99},
                      {"inferText":"2026-07-16", "inferConfidence":0.99},
                      {"inferText":"13:04:48", "inferConfidence":0.99},
                      {"inferText":"7,500", "inferConfidence":0.98},
                      {"inferText":"7,300", "inferConfidence":0.98},
                      {"inferText":"5,500", "inferConfidence":0.98},
                      {"inferText":"결제금액", "inferConfidence":0.99},
                      {"inferText":"25,800", "inferConfidence":0.99}
                    ]
                  }]
                }
                """;

        OcrResponse result = handler.parseOcrResponse(response);

        assertThat(result.getStoreName()).isEqualTo("카페 낭");
        assertThat(result.getAmount()).isEqualTo(25_800L);
        assertThat(result.getPaymentDate()).isEqualTo(LocalDateTime.of(2026, 7, 16, 13, 4, 48));
        assertThat(result.getConfidence()).isGreaterThan(0.75);
        assertThat(result.getCorrectedByLlm()).isFalse();
    }

    @Test
    void 금액_레이블이_없어도_시각은_금액_후보에서_제외한다() {
        String response = """
                {
                  "images": [{
                    "fields": [
                      {"inferText":"카페 낭", "inferConfidence":0.99},
                      {"inferText":"2026-07-16", "inferConfidence":0.99},
                      {"inferText":"13:04:48", "inferConfidence":0.99},
                      {"inferText":"7,500", "inferConfidence":0.99}
                    ]
                  }]
                }
                """;

        OcrResponse result = handler.parseOcrResponse(response);

        assertThat(result.getAmount()).isEqualTo(7_500L);
    }

    @Test
    void 영수증_머리말과_기호를_건너뛰고_실제_상호명을_추출한다() {
        String response = """
                {
                  "images": [{
                    "fields": [
                      {"inferText":"[", "inferConfidence":0.99},
                      {"inferText":"카드판매", "inferConfidence":0.98},
                      {"inferText":"영수증", "inferConfidence":0.98},
                      {"inferText":"]", "inferConfidence":0.99},
                      {"inferText":"[고객용]", "inferConfidence":0.98},
                      {"inferText":"레드버튼(신촌점)", "inferConfidence":0.97},
                      {"inferText":"사업자번호", "inferConfidence":0.99},
                      {"inferText":"2026-09-03", "inferConfidence":0.99},
                      {"inferText":"21:31:44", "inferConfidence":0.99},
                      {"inferText":"합계", "inferConfidence":0.99},
                      {"inferText":"30,100", "inferConfidence":0.99}
                    ]
                  }]
                }
                """;

        OcrResponse result = handler.parseOcrResponse(response);

        assertThat(result.getStoreName()).isEqualTo("레드버튼(신촌점)");
        assertThat(result.getAmount()).isEqualTo(30_100L);
        assertThat(result.getPaymentDate()).isEqualTo(LocalDateTime.of(2026, 9, 3, 21, 31, 44));
    }

    @Test
    void 매장명에_붙은_사업자번호를_제거한다() {
        String response = """
                {
                  "images": [{
                    "fields": [
                      {"inferText":"[매장명]", "inferConfidence":0.99},
                      {"inferText":"토마라멘/708-50-01210", "inferConfidence":0.99},
                      {"inferText":"[주소]", "inferConfidence":0.99},
                      {"inferText":"서울 용산구 원효로89길 13-14", "inferConfidence":0.99},
                      {"inferText":"[매출일]", "inferConfidence":0.99},
                      {"inferText":"2026-04-09", "inferConfidence":0.99},
                      {"inferText":"13:39:15", "inferConfidence":0.99},
                      {"inferText":"합계금액", "inferConfidence":0.99},
                      {"inferText":"14,500", "inferConfidence":0.99}
                    ]
                  }]
                }
                """;

        OcrResponse result = handler.parseOcrResponse(response);

        assertThat(result.getStoreName()).isEqualTo("토마라멘");
        assertThat(result.getAmount()).isEqualTo(14_500L);
        assertThat(result.getPaymentDate()).isEqualTo(LocalDateTime.of(2026, 4, 9, 13, 39, 15));
    }
}
