package com.receipiti.be.domain.report.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class GeminiReportClient {

    private static final String MODEL = "gemini-2.5-flash";
    private static final GenerateContentConfig GENERATION_CONFIG = GenerateContentConfig.builder()
            .temperature(0.0F)
            .responseMimeType(MediaType.APPLICATION_JSON_VALUE)
            .responseSchema(responseSchema())
            .build();

    private final Client client;

    public GeminiReportClient(Client client) {
        this.client = client;
    }

    public String generate(String prompt) {
        GenerateContentResponse response = client.models.generateContent(
                MODEL,
                prompt,
                GENERATION_CONFIG
        );
        return response.text();
    }

    private static Schema responseSchema() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "targetMonth", stringSchema("분석 대상 월(yyyy-MM)"),
                        "totalAmount", nonNegativeIntegerSchema("해당 월 총 지출액"),
                        "transactionCount", nonNegativeIntegerSchema("해당 월 결제 건수"),
                        "anomalyDetected", schema(Type.Known.BOOLEAN),
                        "anomalyReason", stringSchema("이상 소비 탐지 근거 또는 탐지되지 않은 이유"),
                        "frequentSpendingTime", timeAnalysisSchema(),
                        "frequentSpendingDay", dayAnalysisSchema(),
                        "topCategory", categoryAnalysisSchema(),
                        "spendingPatternInsights", Schema.builder()
                                .type(Type.Known.ARRAY)
                                .items(stringSchema("소비 패턴 인사이트 한 문장"))
                                .minItems(1L)
                                .maxItems(3L)
                                .build(),
                        "summary", stringSchema("전체 분석을 요약한 한국어 1~2문장")
                ))
                .required(
                        "targetMonth", "totalAmount", "transactionCount", "anomalyDetected",
                        "anomalyReason", "frequentSpendingTime", "frequentSpendingDay",
                        "topCategory", "spendingPatternInsights", "summary"
                )
                .build();
    }

    private static Schema timeAnalysisSchema() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "timeRange", stringSchema("가장 소비가 잦은 시간대(HH:mm~HH:mm)"),
                        "transactionCount", nonNegativeIntegerSchema("해당 시간대 결제 건수"),
                        "amount", nonNegativeIntegerSchema("해당 시간대 지출액"),
                        "description", stringSchema("시간대 소비 특징 한 문장")
                ))
                .required("timeRange", "transactionCount", "amount", "description")
                .build();
    }

    private static Schema dayAnalysisSchema() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "dayOfWeek", Schema.builder()
                                .type(Type.Known.STRING)
                                .enum_("월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일", "정보 없음")
                                .build(),
                        "transactionCount", nonNegativeIntegerSchema("해당 요일 결제 건수"),
                        "amount", nonNegativeIntegerSchema("해당 요일 지출액"),
                        "description", stringSchema("요일 소비 특징 한 문장")
                ))
                .required("dayOfWeek", "transactionCount", "amount", "description")
                .build();
    }

    private static Schema categoryAnalysisSchema() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "categoryName", stringSchema("지출액이 가장 큰 카테고리명"),
                        "amount", nonNegativeIntegerSchema("해당 카테고리 지출액"),
                        "percentage", Schema.builder()
                                .type(Type.Known.NUMBER)
                                .minimum(0.0)
                                .maximum(100.0)
                                .build(),
                        "description", stringSchema("카테고리 소비 특징 한 문장")
                ))
                .required("categoryName", "amount", "percentage", "description")
                .build();
    }

    private static Schema nonNegativeIntegerSchema(String description) {
        return Schema.builder()
                .type(Type.Known.INTEGER)
                .minimum(0.0)
                .description(description)
                .build();
    }

    private static Schema stringSchema(String description) {
        return Schema.builder()
                .type(Type.Known.STRING)
                .description(description)
                .build();
    }

    private static Schema schema(Type.Known type) {
        return Schema.builder().type(type).build();
    }
}
