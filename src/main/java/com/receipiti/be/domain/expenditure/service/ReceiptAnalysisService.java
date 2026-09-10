package com.receipiti.be.domain.expenditure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.receipiti.be.domain.expenditure.dto.response.OcrResponse;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class ReceiptAnalysisService {

    private static final double OCR_CONFIDENCE_THRESHOLD = 0.75;
    private static final Set<String> RECEIPT_HEADER_WORDS = Set.of(
            "영수증", "카드판매", "카드판매영수증", "고객용", "매장명", "가맹점명"
    );
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp",
            "image/heic",
            "image/heif"
    );
    private static final String PROMPT = """
            이 이미지는 한국의 결제 영수증입니다. 이미지가 회전되어 있으면 올바른 방향으로 읽으세요.
            이미지에 실제로 표시된 값만 사용하여 결제 정보를 추출하세요.

            규칙:
            - storeName은 영수증 상단의 가맹점명 또는 매장명입니다. 카드사명이나 상품명을 사용하지 마세요.
            - amount는 최종 결제금액, 합계, 승인금액 중 실제 지불한 총액입니다.
            - 시각, 전화번호, 사업자번호, 승인번호, 품목 단가는 amount로 사용하지 마세요.
            - paymentDate는 결제 일시이며 yyyy-MM-dd'T'HH:mm:ss 형식으로 반환하세요.
            - 초가 표시되지 않았다면 00을 사용하고, 날짜 또는 시간을 확인할 수 없으면 빈 문자열을 반환하세요.
            - confidence는 전체 결과에 대한 0 이상 1 이하의 신뢰도입니다.
            - 확인할 수 없는 문자열은 빈 문자열, 금액은 0으로 반환하세요. 값을 추측하지 마세요.
            """;

    private final NaverOcrHandler naverOcrHandler;
    private final GeminiReceiptClient geminiReceiptClient;
    private final ObjectMapper objectMapper;

    public ReceiptAnalysisService(
            NaverOcrHandler naverOcrHandler,
            GeminiReceiptClient geminiReceiptClient,
            ObjectMapper objectMapper
    ) {
        this.naverOcrHandler = naverOcrHandler;
        this.geminiReceiptClient = geminiReceiptClient;
        this.objectMapper = objectMapper;
    }

    public OcrResponse analyze(MultipartFile file) {
        validateImage(file);
        OcrResponse ocrResult = null;
        try {
            ocrResult = naverOcrHandler.executeOcr(file);
        } catch (GeneralException exception) {
            log.warn("OCR 영수증 분석 실패, Gemini 보정을 시도합니다. code={}",
                    exception.getCode().getCode());
        }
        if (isReliable(ocrResult)) {
            return withCorrectionMetadata(ocrResult, false);
        }

        try {
            String json = geminiReceiptClient.analyze(PROMPT, file.getBytes(), file.getContentType());
            GeminiReceiptResponse corrected = objectMapper.readValue(json, GeminiReceiptResponse.class);
            OcrResponse result = toOcrResponse(corrected);
            if (!isValid(result)) {
                throw new GeneralException(GeneralErrorCode.RECEIPT_INFORMATION_INSUFFICIENT);
            }
            return result;
        } catch (GeneralException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Gemini 영수증 보정 실패", exception);
            throw new GeneralException(GeneralErrorCode.RECEIPT_ANALYSIS_FAILED);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GeneralException(GeneralErrorCode.IMAGE_REQUIRED);
        }
        if (!SUPPORTED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new GeneralException(GeneralErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
    }

    private boolean isReliable(OcrResponse result) {
        return isValid(result)
                && result.getConfidence() != null
                && Double.isFinite(result.getConfidence())
                && result.getConfidence() >= OCR_CONFIDENCE_THRESHOLD;
    }

    private boolean isValid(OcrResponse result) {
        return result != null
                && isMeaningfulStoreName(result.getStoreName())
                && result.getAmount() != null
                && result.getAmount() > 0
                && result.getPaymentDate() != null
                && result.getConfidence() != null
                && Double.isFinite(result.getConfidence())
                && result.getConfidence() >= 0.0
                && result.getConfidence() <= 1.0;
    }

    private boolean isMeaningfulStoreName(String storeName) {
        if (storeName == null || storeName.isBlank() || "알 수 없는 상호명".equals(storeName)) {
            return false;
        }
        String comparable = storeName
                .replaceAll("[^가-힣A-Za-z0-9]", "")
                .toLowerCase();
        return comparable.length() >= 2 && !RECEIPT_HEADER_WORDS.contains(comparable);
    }

    private OcrResponse toOcrResponse(GeminiReceiptResponse response) {
        LocalDateTime paymentDate = null;
        try {
            if (response.paymentDate() != null && !response.paymentDate().isBlank()) {
                paymentDate = LocalDateTime.parse(response.paymentDate());
            }
        } catch (DateTimeParseException ignored) {
            // 아래 공통 검증에서 정보 부족으로 처리한다.
        }
        return OcrResponse.builder()
                .storeName(response.storeName() == null ? null : response.storeName().trim())
                .amount(response.amount())
                .paymentDate(paymentDate)
                .confidence(response.confidence())
                .correctedByLlm(true)
                .build();
    }

    private OcrResponse withCorrectionMetadata(OcrResponse response, boolean correctedByLlm) {
        return OcrResponse.builder()
                .storeName(response.getStoreName())
                .amount(response.getAmount())
                .paymentDate(response.getPaymentDate())
                .confidence(response.getConfidence())
                .correctedByLlm(correctedByLlm)
                .build();
    }

    private record GeminiReceiptResponse(
            String storeName,
            Long amount,
            String paymentDate,
            Double confidence
    ) {
    }
}
