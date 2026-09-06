package com.receipiti.be.domain.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.receipiti.be.domain.report.dto.response.ReportResponse;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GeminiService {

    private static final Pattern TARGET_MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile(
            "^(?:[01]\\d|2[0-3]):[0-5]\\d~(?:[01]\\d|2[0-3]):[0-5]\\d$"
    );
    private static final Set<String> DAYS = Set.of(
            "월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일", "정보 없음"
    );
    private static final String PROMPT_TEMPLATE = """
            당신은 가계부 소비 데이터를 분석하는 자산 관리 분석가입니다.
            분석 대상 월은 %s입니다. 아래 소비 내역에 실제로 존재하는 정보만 계산하고 해석하세요.

            분석 규칙:
            - 총 지출액과 결제 건수는 입력 데이터 전체를 기준으로 계산합니다.
            - 이상 소비는 평소 패턴과 비교할 근거가 입력에 있을 때만 탐지합니다.
              비교 근거가 없거나 이상이 없으면 anomalyDetected=false로 반환하고 이유를 설명합니다.
            - 가장 소비가 잦은 시간대는 3시간 단위로 묶어 HH:mm~HH:mm 형식으로 반환합니다.
            - 가장 소비가 잦은 요일은 월요일~일요일 중 하나로 반환합니다.
            - 최다 소비 카테고리는 지출액 기준이며 percentage는 총 지출 대비 0~100 사이 비율입니다.
            - 시간, 요일, 카테고리 정보가 없으면 이름은 '정보 없음', 건수와 금액은 0으로 반환합니다.
            - spendingPatternInsights는 중복 없이 핵심 패턴을 1~3개 작성합니다.
            - summary는 수치와 핵심 패턴을 포함한 부드러운 격식체의 한국어 1~2문장으로 작성합니다.
            - 데이터에 없는 상호명, 카테고리, 금액, 날짜를 추측하지 마세요.
            - 마크다운이나 JSON 외 설명을 추가하지 마세요.

            소비 내역:
            %s
            """;

    private final GeminiReportClient geminiClient;
    private final ObjectMapper objectMapper;

    public GeminiService(GeminiReportClient geminiClient, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    public ReportResponse generateExpenditureReport(String targetMonth, String expenditureData) {
        validateRequest(targetMonth, expenditureData);

        try {
            String response = geminiClient.generate(PROMPT_TEMPLATE.formatted(targetMonth, expenditureData));
            ReportResponse result = parseResponse(response);
            validateResult(targetMonth, result);
            return result;
        } catch (GeneralException exception) {
            throw exception;
        } catch (RuntimeException | JsonProcessingException exception) {
            log.warn("Gemini 소비 리포트 생성 실패", exception);
            throw new GeneralException(GeneralErrorCode.AI_REPORT_GENERATION_FAILED);
        }
    }

    private void validateRequest(String targetMonth, String expenditureData) {
        if (targetMonth == null || !TARGET_MONTH_PATTERN.matcher(targetMonth).matches()
                || expenditureData == null || expenditureData.isBlank()) {
            throw new GeneralException(GeneralErrorCode.BAD_REQUEST);
        }
    }

    private ReportResponse parseResponse(String response) throws JsonProcessingException {
        if (response == null || response.isBlank()) {
            throw new GeneralException(GeneralErrorCode.AI_REPORT_GENERATION_FAILED);
        }
        return objectMapper.readValue(response, ReportResponse.class);
    }

    private void validateResult(String targetMonth, ReportResponse result) {
        if (result == null
                || !targetMonth.equals(result.targetMonth())
                || result.totalAmount() == null || result.totalAmount() < 0
                || result.transactionCount() == null || result.transactionCount() < 0
                || isBlank(result.anomalyReason())
                || !validTimeAnalysis(result.frequentSpendingTime())
                || !validDayAnalysis(result.frequentSpendingDay())
                || !validCategoryAnalysis(result.topCategory())
                || result.spendingPatternInsights() == null
                || result.spendingPatternInsights().isEmpty()
                || result.spendingPatternInsights().size() > 3
                || result.spendingPatternInsights().stream().anyMatch(this::isBlank)
                || isBlank(result.summary())) {
            throw new GeneralException(GeneralErrorCode.AI_REPORT_RESPONSE_INVALID);
        }
    }

    private boolean validTimeAnalysis(ReportResponse.TimeAnalysis analysis) {
        return analysis != null
                && analysis.timeRange() != null
                && ("정보 없음".equals(analysis.timeRange())
                || TIME_RANGE_PATTERN.matcher(analysis.timeRange()).matches())
                && analysis.transactionCount() != null && analysis.transactionCount() >= 0
                && analysis.amount() != null && analysis.amount() >= 0
                && !isBlank(analysis.description());
    }

    private boolean validDayAnalysis(ReportResponse.DayAnalysis analysis) {
        return analysis != null
                && DAYS.contains(analysis.dayOfWeek())
                && analysis.transactionCount() != null && analysis.transactionCount() >= 0
                && analysis.amount() != null && analysis.amount() >= 0
                && !isBlank(analysis.description());
    }

    private boolean validCategoryAnalysis(ReportResponse.CategoryAnalysis analysis) {
        return analysis != null
                && !isBlank(analysis.categoryName())
                && analysis.amount() != null && analysis.amount() >= 0
                && analysis.percentage() != null && Double.isFinite(analysis.percentage())
                && analysis.percentage() >= 0.0 && analysis.percentage() <= 100.0
                && !isBlank(analysis.description());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
