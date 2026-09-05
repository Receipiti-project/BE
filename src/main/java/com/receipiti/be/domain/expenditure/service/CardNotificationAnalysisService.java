package com.receipiti.be.domain.expenditure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.receipiti.be.domain.expenditure.dto.response.CardNotificationAnalysisResponse;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import java.io.IOException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class CardNotificationAnalysisService {

    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp",
            "image/heic",
            "image/heif"
    );
    private static final String PROMPT = """
            이 이미지는 카드사의 결제 알림 화면일 수 있습니다.
            이미지에 실제로 표시된 내용만 읽어 소비 정보를 추출하세요.
            보이지 않는 카드사, 가맹점, 날짜를 추측하지 마세요.

            규칙:
            - 결제 승인 또는 결제 취소 알림이 아니면 paymentNotification은 false입니다.
            - cardCompany는 카드사 이름, storeName은 결제 가맹점명입니다.
            - amount는 쉼표와 통화 기호를 제외한 정수 금액입니다.
            - paymentDateTime은 화면에서 확인 가능한 형태 그대로 반환합니다.
              날짜와 시간이 모두 명확하면 yyyy-MM-dd'T'HH:mm:ss 형식을 사용합니다.
            - currency는 KRW, USD 같은 ISO 통화 코드입니다. 원화 표시는 KRW입니다.
            - approvalStatus는 APPROVED, CANCELED, UNKNOWN 중 하나입니다.
            - confidence는 전체 추출 결과에 대한 0 이상 1 이하의 신뢰도입니다.
            - 확인할 수 없는 문자열은 빈 문자열, 금액은 0으로 반환하세요.
            """;

    private final GeminiCardNotificationClient geminiClient;
    private final ObjectMapper objectMapper;

    public CardNotificationAnalysisService(
            GeminiCardNotificationClient geminiClient,
            ObjectMapper objectMapper
    ) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    public CardNotificationAnalysisResponse analyze(MultipartFile file) {
        validateImage(file);

        try {
            String response = geminiClient.analyze(PROMPT, file.getBytes(), file.getContentType());
            CardNotificationAnalysisResponse result = parseResponse(response);
            validateResult(result);
            return result;
        } catch (GeneralException exception) {
            throw exception;
        } catch (RuntimeException | IOException exception) {
            log.warn("Gemini 카드 알림 분석 실패", exception);
            throw new GeneralException(GeneralErrorCode.CARD_NOTIFICATION_ANALYSIS_FAILED);
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

    private CardNotificationAnalysisResponse parseResponse(String response) throws IOException {
        if (response == null || response.isBlank()) {
            throw new GeneralException(GeneralErrorCode.CARD_NOTIFICATION_ANALYSIS_FAILED);
        }
        return objectMapper.readValue(response, CardNotificationAnalysisResponse.class);
    }

    private void validateResult(CardNotificationAnalysisResponse result) {
        if (!result.paymentNotification()
                || result.storeName() == null
                || result.storeName().isBlank()
                || result.amount() == null
                || result.amount() <= 0
                || result.confidence() == null
                || !Double.isFinite(result.confidence())
                || result.confidence() < 0.0
                || result.confidence() > 1.0) {
            throw new GeneralException(GeneralErrorCode.CARD_NOTIFICATION_INFORMATION_INSUFFICIENT);
        }
    }
}
